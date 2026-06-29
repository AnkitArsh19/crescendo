"""
Workflow Planner Agent
======================
Calls the Groq API (llama-3.3-70b-versatile) to convert a natural-language
prompt into a structured WorkflowSpec.

The `context` dict can carry any extra information the Java backend forwards,
e.g.:
  - "connected_apps": ["Gmail", "Slack", "Notion"]   — apps the user has linked
  - "existing_workflows": ["Morning digest", "PR alert"] — names already in use

These are injected into the system prompt so the model produces a workflow
that is immediately actionable for this specific user.
"""

import json
import os
import logging
from typing import Any, Dict, Optional

from groq import Groq

from app.catalog_sync import app_state
from app.schemas.workflow import WorkflowDraftResponse, WorkflowSpec

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# System prompt template
# ---------------------------------------------------------------------------

_SYSTEM_BASE = """\
You are an expert automation workflow designer for Crescendo, an automation platform
similar to Zapier or Make.com.

Your job is to convert a user's natural-language description into a precise,
structured workflow specification as JSON.

RULES:
1. Respond with ONLY valid JSON — no markdown fences, no extra text.
2. The JSON must conform exactly to this schema:
   {
     "workflow_name": "string",
     "description":   "string",
     "trigger": {
       "app_name":     "string",
       "trigger_type": "string",
       "config":       {}
     },
     "actions": [
       {
         "app_name":    "string",
         "action_type": "string",
         "config":      {}
       }
     ]
   }
3. CRITICAL: For app_name, trigger_type, and action_type, you MUST ONLY use the exact keys provided in the "AVAILABLE APPS, TRIGGERS, AND ACTIONS" catalog below. Do not invent your own!
   - Use the `appKey` for `app_name`
   - Use one of the `Triggers` keys for `trigger_type`
   - Use one of the `Actions` keys for `action_type`
4. Populate "config" with sensible default keys for that integration.
5. If the user mentions apps that are NOT in their connected apps list, still use them (from the catalog) but keep configs minimal.
6. Choose a concise, descriptive workflow_name.
"""


def _build_system_prompt(context: Dict[str, Any]) -> str:
    """Append user-specific context to the base system prompt."""
    parts = [_SYSTEM_BASE]

    connected_apps: list = context.get("connected_apps", [])
    if connected_apps:
        parts.append(
            f"\nUSER'S CONNECTED APPS (prefer these): {', '.join(connected_apps)}"
        )

    existing_workflows: list = context.get("existing_workflows", [])
    if existing_workflows:
        parts.append(
            f"\nEXISTING WORKFLOW NAMES (avoid duplicates): {', '.join(existing_workflows)}"
        )
        
    app_catalog = app_state.get("catalog", [])
    if app_catalog:
        parts.append("\nAVAILABLE APPS, TRIGGERS, AND ACTIONS (You MUST use these exact keys):")
        for app in app_catalog:
            parts.append(f"- App: '{app.get('appKey')}' ({app.get('name')})")
            parts.append(f"  Triggers: {app.get('triggers', [])}")
            parts.append(f"  Actions: {app.get('actions', [])}")

    # Forward any other context keys as free-form notes
    extra_keys = {k: v for k, v in context.items()
                  if k not in ("connected_apps", "existing_workflows")}
    if extra_keys:
        parts.append(f"\nADDITIONAL CONTEXT: {json.dumps(extra_keys)}")

    return "\n".join(parts)


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def plan_workflow(
    prompt: str,
    context: Dict[str, Any],
    user_id: str,
) -> WorkflowDraftResponse:
    """
    Call Groq and parse the response into a WorkflowDraftResponse.

    Returns a WorkflowDraftResponse with success=False and an error message
    if anything goes wrong, so callers always get a safe result.
    """
    api_key = os.getenv("GROQ_API_KEY", "")
    if not api_key:
        logger.error("GROQ_API_KEY is not set")
        return WorkflowDraftResponse(success=False, error="AI service is not configured (missing API key).")

    client = Groq(api_key=api_key)
    system_prompt = _build_system_prompt(context)

    try:
        chat_response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user",   "content": prompt},
            ],
            temperature=0.3,      # low temperature → deterministic, schema-faithful output
            max_tokens=1024,
            response_format={"type": "json_object"},
        )
    except Exception as exc:
        logger.exception("Groq API call failed for user %s", user_id)
        return WorkflowDraftResponse(success=False, error=f"AI service error: {exc}")

    raw_text: Optional[str] = chat_response.choices[0].message.content
    if not raw_text:
        return WorkflowDraftResponse(success=False, error="AI returned an empty response.")

    logger.info(f"Raw JSON from LLM: {raw_text}")

    # Parse and validate against WorkflowSpec
    try:
        spec_data = json.loads(raw_text)
        workflow_spec = WorkflowSpec(**spec_data)
    except (json.JSONDecodeError, ValueError) as exc:
        logger.error("Failed to parse AI response for user %s: %s\nRaw: %s", user_id, exc, raw_text)
        return WorkflowDraftResponse(success=False, error=f"AI returned an unparseable response: {exc}")

    logger.info("Successfully generated AI workflow spec: %s", workflow_spec.model_dump_json(indent=2))
    logger.info("Workflow draft created for user %s: %s", user_id, workflow_spec.workflow_name)
    return WorkflowDraftResponse(success=True, workflow_spec=workflow_spec)
