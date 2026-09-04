"""
Stage 2 — App + Action Resolver  (async)
==========================================
Uses the high-reasoning model (REASONING_MODEL) to map intent descriptions to exact catalog keys.

Phase 3 changes:
  - Fully async (AsyncGroq)
  - Catalog filter handles enriched catalog format (triggers/actions are objects,
    not bare strings — Java now returns {key, name, description, configSchema})
  - Config is always {} — Stage 3 (configurator) fills it in with schema-aware binding
  - client_id is assigned to each step ("step_0", "step_1", …)
  - When intent.has_branching=True, the resolver outputs an edges[] array and
    auto-injects logic:if / logic:switch from the catalog
  - resolve_steps_corrected() is a correction-retry variant used by the graph's
    correction node
"""

import json
import logging
import re
from typing import List, Optional

from groq import AsyncGroq

from app.agents.client import get_groq_client
from app.agents.models import REASONING_MODEL
from app.audit.logger import audit_log
from app.catalog_sync import app_state
from app.schemas.workflow import IntentResult, ResolvedStep, WorkflowEdge

logger = logging.getLogger(__name__)

_MAX_CATALOG_APPS = 20

_STOPWORDS = {
    "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for",
    "of", "with", "by", "from", "is", "it", "my", "me", "get", "set",
    "new", "all", "any", "do", "when", "then", "that", "this",
}


# ---------------------------------------------------------------------------
# Catalog helpers
# ---------------------------------------------------------------------------

def _extract_keywords(text: str) -> set[str]:
    words = re.findall(r"[a-z]+", text.lower())
    return {w for w in words if w not in _STOPWORDS and len(w) > 2}


def _op_text(ops: list) -> list[str]:
    """Extract searchable text from enriched or legacy operation entries."""
    parts = []
    for op in ops:
        if isinstance(op, dict):
            parts.append(f"{op.get('key', '')} {op.get('name', '')}")
        elif isinstance(op, str):
            parts.append(op)
    return parts


def _filter_catalog(intent: IntentResult, catalog: list, max_apps: int = _MAX_CATALOG_APPS) -> list:
    """
    Score each catalog app by keyword overlap with the intent descriptions.
    Returns the top-max_apps entries.  Always includes 'logic' app when
    has_branching=True so the resolver can use if/switch nodes.
    """
    combined_text = " ".join([intent.trigger_description] + intent.action_descriptions)
    keywords = _extract_keywords(combined_text)

    scored: list[tuple[int, dict]] = []
    logic_app: Optional[dict] = None

    for app in catalog:
        app_key = app.get("appKey", "")
        if app_key == "logic":
            logic_app = app

        trigger_texts = _op_text(app.get("triggers", []))
        action_texts  = _op_text(app.get("actions",  []))
        app_text = " ".join([app_key, app.get("name", "")] + trigger_texts + action_texts).lower()
        score = sum(1 for kw in keywords if kw in app_text)
        scored.append((score, app))

    scored.sort(key=lambda x: (-x[0], x[1].get("appKey", "")))
    top = [app for _, app in scored[:max_apps]]

    # Always include logic app for branching workflows
    if intent.has_branching and logic_app and logic_app not in top:
        top.append(logic_app)

    logger.debug("Catalog filtered: %d apps → %d selected. Top 5: %s",
                 len(catalog), len(top), [a.get("appKey") for a in top[:5]])
    return top


def _format_ops(ops: list) -> str:
    """Format enriched operation entries for the resolver prompt."""
    lines = []
    for op in ops:
        if isinstance(op, dict):
            key  = op.get("key", "")
            name = op.get("name", "")
            desc = op.get("description", "")
            lines.append(f'      "{key}"  — {name}' + (f" ({desc[:60]})" if desc else ""))
        elif isinstance(op, str):
            lines.append(f'      "{op}"')
    return "\n".join(lines) if lines else "      (none)"


# ---------------------------------------------------------------------------
# System prompt builder
# ---------------------------------------------------------------------------

def _build_resolver_prompt(intent: IntentResult, filtered_catalog: list) -> str:
    branch_rules = ""
    if intent.has_branching:
        branch_rules = (
            "\n7. When the workflow includes conditional branching, you MUST include a "
            "'logic:if' or 'logic:switch' action step from the 'logic' appKey.  "
            "Also return an 'edges' array that connects steps: each edge has "
            "{source_step_id, target_step_id, source_handle}.  "
            "For logic:if use source_handle 'true' or 'false'.  "
            "For non-branching edges source_handle is null.  "
            "The schema for branching output:"
            "\n   {\n"
            '     "trigger": { "client_id": "step_0", "app_key": "...", "action_key": "...", "display_name": "...", "config": {} },\n'
            '     "actions": [\n'
            '       { "client_id": "step_1", "app_key": "logic", "action_key": "logic:if", "display_name": "If", "config": {} },\n'
            '       { "client_id": "step_2", "app_key": "...", "action_key": "...", "display_name": "...", "config": {} },\n'
            '       { "client_id": "step_3", "app_key": "...", "action_key": "...", "display_name": "...", "config": {} }\n'
            "     ],\n"
            '     "edges": [\n'
            '       { "source_step_id": "step_0", "target_step_id": "step_1", "source_handle": null },\n'
            '       { "source_step_id": "step_1", "target_step_id": "step_2", "source_handle": "true" },\n'
            '       { "source_step_id": "step_1", "target_step_id": "step_3", "source_handle": "false" }\n'
            "     ]\n"
            "   }"
        )

    base_schema = (
        '   {\n'
        '     "trigger": { "client_id": "step_0", "app_key": "string", "action_key": "string", "display_name": "string", "config": {} },\n'
        '     "actions": [ { "client_id": "step_N", "app_key": "string", "action_key": "string", "display_name": "string", "config": {} } ],\n'
        '     "edges":   []\n'
        '   }'
    )

    lines = [
        "You are an app and action resolver for an automation platform.",
        "",
        "Given a trigger description and action descriptions, resolve each to EXACT",
        "appKey and triggerKey/actionKey values from the catalog below.",
        "",
        "RULES:",
        "1. Respond with ONLY valid JSON — no markdown fences, no extra text.",
        "2. NEVER invent keys — use ONLY appKey/triggerKey/actionKey values listed below.",
        "3. The JSON must conform exactly to this schema:",
        base_schema,
        "4. For the trigger object, action_key must be a triggerKey for that app.",
        "5. For each action object, action_key must be an actionKey for that app.",
        "6. Set config to {} for every step — a dedicated configurator stage will fill it in.",
        "7. Assign client_id as 'step_0' for trigger, 'step_1', 'step_2', … for actions.",
        "8. When edges is empty (non-branching), include it as an empty array [].",
        branch_rules,
        "",
        "AVAILABLE APPS (use ONLY these exact key values):",
    ]

    for app in filtered_catalog:
        lines.append(f"\n  appKey: \"{app.get('appKey')}\"  ({app.get('name', '')})")
        trigger_ops = app.get("triggers", [])
        action_ops  = app.get("actions",  [])
        if trigger_ops:
            lines.append("    triggerKeys:")
            lines.append(_format_ops(trigger_ops))
        if action_ops:
            lines.append("    actionKeys:")
            lines.append(_format_ops(action_ops))

    lines += [
        "",
        "INTENT TO RESOLVE:",
        f"  Trigger: {intent.trigger_description}",
    ]
    for i, ad in enumerate(intent.action_descriptions, 1):
        lines.append(f"  Action {i}: {ad}")
    if intent.has_branching and intent.branch_description:
        lines.append(f"  Branching logic: {intent.branch_description}")

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Internal parse helper
# ---------------------------------------------------------------------------

def _parse_resolver_output(raw: str, user_id: str) -> tuple[ResolvedStep, list[ResolvedStep], list[WorkflowEdge]]:
    try:
        data = json.loads(raw)
        trigger_data = data["trigger"]
        trigger_step = ResolvedStep(
            app_key=trigger_data["app_key"],
            action_key=trigger_data["action_key"],
            display_name=trigger_data.get("display_name", trigger_data["action_key"]),
            config={},
            client_id=trigger_data.get("client_id", "step_0"),
        )
        action_steps = [
            ResolvedStep(
                app_key=a["app_key"],
                action_key=a["action_key"],
                display_name=a.get("display_name", a["action_key"]),
                config={},
                client_id=a.get("client_id", f"step_{i + 1}"),
            )
            for i, a in enumerate(data.get("actions", []))
        ]
        edges = [
            WorkflowEdge(
                source_step_id=e["source_step_id"],
                target_step_id=e["target_step_id"],
                source_handle=e.get("source_handle"),
            )
            for e in data.get("edges", [])
        ]
        return trigger_step, action_steps, edges
    except (json.JSONDecodeError, KeyError, ValueError) as exc:
        logger.error("Resolver parse error for user %s: %s\nRaw: %s", user_id, exc, raw)
        raise RuntimeError(f"Resolver returned unparseable JSON: {exc}") from exc


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

async def resolve_steps(
    intent: IntentResult,
    user_id: str,
    groq_client: Optional[AsyncGroq] = None,
) -> tuple[ResolvedStep, list[ResolvedStep], list[WorkflowEdge]]:
    """
    Stage 2: resolve intent descriptions to exact catalog keys.

    Returns
    -------
    (trigger_step, action_steps, edges) — ResolvedStep objects and WorkflowEdge list.
    Raises RuntimeError on failure.
    """
    client = groq_client or get_groq_client()
    catalog = app_state.get("catalog", [])
    filtered = _filter_catalog(intent, catalog)
    system_prompt = _build_resolver_prompt(intent, filtered)

    try:
        response = await client.chat.completions.create(
            model=REASONING_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user",   "content": "Resolve the intent to catalog keys now."},
            ],
            temperature=0.1,
            max_tokens=1024,
            response_format={"type": "json_object"},
        )
    except Exception as exc:
        logger.exception("Stage 2 (resolver) Groq call failed for user %s", user_id)
        raise RuntimeError(f"Resolver LLM error: {exc}") from exc

    usage = response.usage
    audit_log(
        user_id=user_id,
        stage="resolver",
        model=REASONING_MODEL,
        prompt_tokens=usage.prompt_tokens if usage else 0,
        completion_tokens=usage.completion_tokens if usage else 0,
        validation_passed=True,
        error=None,
    )

    raw = response.choices[0].message.content or ""
    logger.debug("Stage 2 raw JSON: %s", raw)
    return _parse_resolver_output(raw, user_id)


async def resolve_steps_corrected(
    intent: IntentResult,
    validation_errors: List[str],
    user_id: str,
    groq_client: Optional[AsyncGroq] = None,
) -> tuple[ResolvedStep, list[ResolvedStep], list[WorkflowEdge]]:
    """
    Correction retry: re-run the resolver with validation errors injected as context.
    Called by the graph's correction node on first validation failure.
    """
    from app.agents.validator import format_correction_context  # avoid circular import
    client = groq_client or get_groq_client()
    catalog = app_state.get("catalog", [])
    filtered = _filter_catalog(intent, catalog)
    base_prompt = _build_resolver_prompt(intent, filtered)
    correction_note = format_correction_context(validation_errors)
    system_prompt = base_prompt + "\n\n" + correction_note

    try:
        response = await client.chat.completions.create(
            model=REASONING_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user",   "content": "Correct and re-resolve the intent to valid catalog keys."},
            ],
            temperature=0.1,
            max_tokens=1024,
            response_format={"type": "json_object"},
        )
    except Exception as exc:
        logger.exception("Correction retry Groq call failed for user %s", user_id)
        raise RuntimeError(f"Correction LLM error: {exc}") from exc

    usage = response.usage
    audit_log(
        user_id=user_id,
        stage="correction",
        model=REASONING_MODEL,
        prompt_tokens=usage.prompt_tokens if usage else 0,
        completion_tokens=usage.completion_tokens if usage else 0,
        validation_passed=False,
        error=f"{len(validation_errors)} error(s) corrected",
    )

    raw = response.choices[0].message.content or ""
    logger.debug("Correction raw JSON: %s", raw)
    return _parse_resolver_output(raw, user_id)


# Keep legacy exports so planner shim still compiles
_build_resolver_prompt = _build_resolver_prompt  # noqa: F811
_filter_catalog = _filter_catalog                 # noqa: F811
