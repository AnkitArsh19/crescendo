"""Focused workflow edits for an existing Phase 3 session.

This path intentionally replaces the expensive intent -> resolver -> configurator
chain with one bounded LLM call.  It is used only for obvious refinement prompts
and only when a validated WorkflowSpec was checkpointed for the session.
"""

from __future__ import annotations

import json
import logging
from typing import Any, Optional

from groq import AsyncGroq

from app.agents.client import get_groq_client
from app.agents.models import REASONING_MODEL
from app.audit.logger import audit_log
from app.catalog_sync import app_state
from app.schemas.workflow import WorkflowSpec

logger = logging.getLogger(__name__)

_MODEL = REASONING_MODEL
_REFINEMENT_PREFIXES = (
    "also ", "add ", "change ", "replace ", "remove ", "update ", "make ",
    "actually", "instead", "set ", "switch ", "modify ",
)


def is_refinement_prompt(sanitized_prompt: str) -> bool:
    """Cheap deterministic gate; unrelated follow-ups use the full pipeline."""
    prompt = (sanitized_prompt.replace("<user_request>", "")
              .replace("</user_request>", "").strip().lower())
    return prompt.startswith(_REFINEMENT_PREFIXES)


def _catalog_keys() -> list[dict[str, Any]]:
    """Send only operation keys, never the whole schema/resource payload again."""
    entries = []
    for app in app_state.get("catalog", []):
        operations = {}
        for label in ("triggers", "actions"):
            operations[label] = [
                op.get("key", "") if isinstance(op, dict) else op
                for op in app.get(label, [])
            ]
        entries.append({"appKey": app.get("appKey", ""), **operations})
    return entries


async def apply_workflow_update(
    current_spec: WorkflowSpec,
    sanitized_prompt: str,
    user_id: str,
    groq_client: Optional[AsyncGroq] = None,
) -> WorkflowSpec:
    """Apply a small user-requested edit while retaining untouched configuration."""
    client = groq_client or get_groq_client()
    system = """You edit an existing automation workflow.
Return ONLY a complete valid JSON WorkflowSpec. Apply only the requested change;
preserve every unrelated trigger, action, configuration value, edge, and workflow
name. Use only keys supplied in the catalog. Edges must reference step_0 for the
trigger and step_1...step_N for actions. Do not ask questions or redesign the
workflow. If the requested edit cannot be made, return the original spec exactly.
"""
    response = await client.chat.completions.create(
        model=_MODEL,
        messages=[
            {"role": "system", "content": system},
            {"role": "user", "content": json.dumps({
                "existing_workflow": current_spec.model_dump(),
                "requested_update": sanitized_prompt,
                "catalog": _catalog_keys(),
            }, ensure_ascii=False)},
        ],
        temperature=0.1,
        max_tokens=2048,
        response_format={"type": "json_object"},
    )
    usage = response.usage
    audit_log(
        user_id=user_id, stage="continuation_update", model=_MODEL,
        prompt_tokens=usage.prompt_tokens if usage else 0,
        completion_tokens=usage.completion_tokens if usage else 0,
        validation_passed=True, error=None,
    )
    raw = response.choices[0].message.content or "{}"
    try:
        return WorkflowSpec.model_validate_json(raw)
    except Exception as exc:
        logger.warning(
            "continuation_update: LLM returned unparseable JSON for user %s (%s) — "
            "returning original spec unchanged.", user_id, exc
        )
        return current_spec

