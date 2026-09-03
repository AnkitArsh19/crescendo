"""
Stage 5 — Explanation Generator  (async)
==========================================
Uses llama-3.1-8b-instant to produce a plain-English summary.
Phase 3: fully async using AsyncGroq.
"""

import logging
from typing import Optional

from groq import AsyncGroq

from app.agents.client import get_groq_client
from app.agents.models import FAST_MODEL
from app.audit.logger import audit_log
from app.schemas.workflow import WorkflowSpec

logger = logging.getLogger(__name__)

_EXPLAINER_SYSTEM = """\
You are a friendly assistant that explains automation workflows to non-technical users.

Given a structured workflow specification (JSON), write a concise plain-English
description of exactly what the workflow does.

RULES:
1. Start the response with "This workflow will ".
2. Write 2-4 sentences maximum.
3. Mention the trigger event and each action clearly.
4. Use plain language — no JSON keys, no technical jargon.
5. Do NOT wrap the response in quotes or markdown.
"""


async def generate_explanation(
    workflow_spec: WorkflowSpec,
    user_id: str,
    groq_client: Optional[AsyncGroq] = None,
) -> str:
    """
    Stage 5: generate a plain-English explanation of the workflow.
    Returns a safe fallback string on error rather than raising.
    """
    client = groq_client or get_groq_client()

    trigger = workflow_spec.trigger
    actions = workflow_spec.actions
    spec_summary = (
        f"Workflow name: {workflow_spec.workflow_name}\n"
        f"Trigger: app={trigger.app_key}, event={trigger.trigger_key}, config={trigger.config}\n"
    )
    for i, action in enumerate(actions, 1):
        spec_summary += (
            f"Action {i}: app={action.app_key}, action={action.action_key}, config={action.config}\n"
        )
    if workflow_spec.edges:
        spec_summary += f"Edges (DAG): {len(workflow_spec.edges)} connection(s)\n"

    try:
        response = await client.chat.completions.create(
            model=FAST_MODEL,
            messages=[
                {"role": "system", "content": _EXPLAINER_SYSTEM},
                {"role": "user",   "content": spec_summary},
            ],
            temperature=0.4,
            max_tokens=256,
        )
    except Exception as exc:
        logger.warning("Stage 5 (explainer) LLM call failed for user %s: %s", user_id, exc)
        return _fallback_explanation(workflow_spec)

    usage = response.usage
    audit_log(
        user_id=user_id,
        stage="explainer",
        model=FAST_MODEL,
        prompt_tokens=usage.prompt_tokens if usage else 0,
        completion_tokens=usage.completion_tokens if usage else 0,
        validation_passed=True,
        error=None,
    )

    text = (response.choices[0].message.content or "").strip()
    if not text:
        return _fallback_explanation(workflow_spec)

    logger.info("Stage 5 explanation generated for user %s", user_id)
    return text


def _fallback_explanation(workflow_spec: WorkflowSpec) -> str:
    trigger = workflow_spec.trigger
    action_parts = [f"{a.action_key} via {a.app_key}" for a in workflow_spec.actions]
    actions_str = ", then ".join(action_parts) if action_parts else "perform configured actions"
    return (
        f"This workflow will monitor {trigger.app_key} for {trigger.trigger_key} events, "
        f"then {actions_str}."
    )
