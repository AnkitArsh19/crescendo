"""
Stage 3 — Context-Aware Configurator  (new in Phase 3)
========================================================
Uses the high-reasoning model (REASONING_MODEL) to fill in the configuration for every resolved
step, using:
  1. The full configSchema injected by InternalCatalogController (tells the AI
     exactly what fields exist and what type they are)
  2. The user's live resources from AiContextService (real channel IDs, spreadsheet
     IDs, etc.) — ensures IDs not guessed label strings are emitted
  3. The user's connected account IDs for binding connectionId fields

One batched LLM call handles ALL steps — avoids N round trips.

CRITICAL rules enforced in the system prompt:
  - dynamic_dropdown fields MUST use the resource's 'id', not its label
  - logic:if / logic:switch config uses the shapeHint embedded in the catalog
  - Fields not in configSchema are silently dropped
"""

import json
import logging
from typing import Any, Dict, List, Optional, Tuple

from groq import AsyncGroq

from app.agents.client import get_groq_client
from app.agents.models import REASONING_MODEL
from app.audit.logger import audit_log
from app.catalog_sync import app_state
from app.schemas.workflow import IntentResult, ResolvedStep

logger = logging.getLogger(__name__)

_MODEL = REASONING_MODEL


# ---------------------------------------------------------------------------
# Catalog schema index
# ---------------------------------------------------------------------------

def _build_schema_index(catalog: list) -> Dict[str, Dict[str, list]]:
    """
    Returns { appKey: { operationKey: [configSchema fields] } }
    Works with the enriched catalog format Java now emits.
    """
    index: Dict[str, Dict[str, list]] = {}
    for app in catalog:
        app_key = app.get("appKey", "")
        if not app_key:
            continue
        index[app_key] = {}
        for op in app.get("triggers", []):
            if isinstance(op, dict):
                index[app_key][op.get("key", "")] = op.get("configSchema", [])
        for op in app.get("actions", []):
            if isinstance(op, dict):
                index[app_key][op.get("key", "")] = op.get("configSchema", [])
    return index


# ---------------------------------------------------------------------------
# Context helpers
# ---------------------------------------------------------------------------

def _resources_for_app(app_key: str, connection_id: str, context: Dict[str, Any]) -> List[Dict]:
    """Return resource items for the given (appKey, connectionId) pair, annotated with resourceType."""
    items = []
    for resource_block in context.get("resources", []):
        if resource_block.get("appKey") == app_key and (not connection_id or resource_block.get("connectionId") == connection_id):
            res_type = resource_block.get("resourceType", "")
            for item in resource_block.get("items", []):
                items.append({**item, "resourceType": res_type})
    return items


def _connection_for_app(app_key: str, context: Dict[str, Any]) -> Optional[Dict]:
    """Return the first connection for the given appKey, or None."""
    for conn in context.get("connections", []):
        if conn.get("appKey") == app_key:
            return conn
    return None


# ---------------------------------------------------------------------------
# Prompt builder
# ---------------------------------------------------------------------------

def _build_configurator_prompt(
    user_prompt: str,
    trigger_step: ResolvedStep,
    action_steps: List[ResolvedStep],
    schema_index: Dict[str, Dict[str, list]],
    context: Dict[str, Any],
) -> str:
    steps_json: List[Dict[str, Any]] = []

    for idx, step in enumerate([(trigger_step, "trigger")] + [(s, "action") for s in action_steps]):
        resolved, step_type = step
        schema = schema_index.get(resolved.app_key, {}).get(resolved.action_key, [])
        conn = _connection_for_app(resolved.app_key, context)
        resources = _resources_for_app(
            resolved.app_key,
            conn.get("connectionId", "") if conn else "",
            context,
        )
        entry: Dict[str, Any] = {
            "step_index": idx,
            "type": step_type,
            "app_key": resolved.app_key,
            "action_key": resolved.action_key,
            "configSchema": schema,
        }
        if conn:
            entry["connection"] = {"connectionId": conn["connectionId"], "label": conn.get("label", "")}
        if resources:
            entry["available_resources"] = resources[:50]   # cap at 50 items per step
        steps_json.append(entry)

    system = f"""\
You are a configuration specialist for an automation platform.

Given resolved workflow steps, their configSchema definitions, and the user's
live resources, fill in the configuration for every step.

USER'S ORIGINAL REQUEST: {user_prompt}

USER'S CONNECTIONS:
{json.dumps(context.get("connections", []), indent=2)}

RULES:
1. Respond with ONLY valid JSON, no markdown, no extra text.
2. The JSON MUST have this exact shape:
   {{
     "trigger": {{ "config": {{}} }},
     "actions": [ {{ "config": {{}} }}, ... ]  // same order as input steps
   }}
3. For fields whose configSchema type is "dynamic_dropdown":
   - Match the field's "resourceType" against available_resources items having the matching "resourceType".
   - You MUST use the resource's "id" value — NEVER use the label string.
   - If the user prompt mentions a specific resource name or purpose, choose the best matching resource.
   - Otherwise, if matching resources exist for that resourceType, choose the first available resource.
   - If no matching resources exist for that resourceType, set the value to null.
4. For "text" or "textarea" fields (like content, message, text):
   - Provide a helpful, concise default message tailored to the workflow trigger and user request.
5. If a required field cannot be determined (no matching resource, no context),
   set its value to null — do NOT omit the key.
6. For logic:if and logic:switch steps, read the shapeHint in the configSchema and
   produce config that exactly matches that shape.
7. Do NOT add keys that are not in configSchema.
8. For steps with an empty configSchema, return an empty config {{}}.

STEPS TO CONFIGURE:
{json.dumps(steps_json, indent=2)}
"""
    return system


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

async def configure_steps(
    trigger_step: ResolvedStep,
    action_steps: List[ResolvedStep],
    context: Dict[str, Any],
    user_prompt: str,
    user_id: str,
    groq_client: Optional[AsyncGroq] = None,
) -> Tuple[ResolvedStep, List[ResolvedStep]]:
    """
    Stage 3: fill in config for every resolved step using schemas and live resources.

    Returns updated (trigger_step, action_steps) with config populated.
    Never raises — on any failure it returns the steps with config unchanged (safe degradation).
    """
    client = groq_client or get_groq_client()
    catalog = app_state.get("catalog", [])
    schema_index = _build_schema_index(catalog)

    system_prompt = _build_configurator_prompt(
        user_prompt, trigger_step, action_steps, schema_index, context
    )

    try:
        response = await client.chat.completions.create(
            model=_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user",   "content": "Fill in the configuration for each step now."},
            ],
            temperature=0.1,
            max_tokens=2048,
            response_format={"type": "json_object"},
        )
    except Exception as exc:
        logger.warning("Stage 3 (configurator) Groq call failed for user %s: %s", user_id, exc)
        audit_log(user_id, "configurator", _MODEL, 0, 0, False, str(exc))
        return trigger_step, action_steps   # graceful degradation

    usage = response.usage
    audit_log(
        user_id=user_id,
        stage="configurator",
        model=_MODEL,
        prompt_tokens=usage.prompt_tokens if usage else 0,
        completion_tokens=usage.completion_tokens if usage else 0,
        validation_passed=True,
        error=None,
    )

    raw = response.choices[0].message.content or ""
    logger.debug("Stage 3 raw JSON: %s", raw)

    try:
        data = json.loads(raw)

        # Update trigger config
        trigger_config = data.get("trigger", {}).get("config", {})
        updated_trigger = trigger_step.model_copy(update={"config": trigger_config})

        # Update each action config
        actions_data = data.get("actions", [])
        updated_actions = []
        for i, step in enumerate(action_steps):
            if i < len(actions_data):
                cfg = actions_data[i].get("config", {})
            else:
                cfg = {}
            updated_actions.append(step.model_copy(update={"config": cfg}))

        logger.info("Stage 3 configured %d step(s) for user %s", 1 + len(updated_actions), user_id)
        return updated_trigger, updated_actions

    except (json.JSONDecodeError, KeyError, ValueError) as exc:
        logger.error("Stage 3 parse error for user %s: %s\nRaw: %s", user_id, exc, raw)
        return trigger_step, action_steps   # graceful degradation
