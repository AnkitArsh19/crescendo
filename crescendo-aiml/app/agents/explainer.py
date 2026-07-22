"""
Stage 5 — Explanation Generator
=================================
Uses llama-3.1-8b-instant (fast / cheap) to produce a concise plain-English
summary of the generated workflow.

The explanation is returned to the user as part of WorkflowDraftResponse
so they can immediately understand what the workflow does without reading JSON.
"""

import logging
import os
from typing import Optional

from groq import Groq

from app.schemas.workflow import WorkflowSpec
from app.audit.logger import audit_log

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


def generate_explanation(
    workflow_spec: WorkflowSpec,
    user_id: str,
    groq_client: Optional[Groq] = None,
) -> str:
    """
    Stage 5: generate a plain-English explanation of the workflow.

    Parameters
    ----------
    workflow_spec:
        The validated, final WorkflowSpec.
    user_id:
        For audit logging.
    groq_client:
        Optional pre-built Groq client. If None, built from GROQ_API_KEY.

    Returns
    -------
    str — the explanation text.  Returns a safe fallback string on error
    rather than raising, so a failed explanation never blocks the response.
    """
    if groq_client is None:
        api_key = os.getenv("GROQ_API_KEY", "")
        if not api_key:
            logger.error("GROQ_API_KEY not set; skipping explanation")
            return _fallback_explanation(workflow_spec)
        groq_client = Groq(api_key=api_key)

    # Summarise the spec into a compact description for the prompt
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

    try:
        response = groq_client.chat.completions.create(
            model="llama-3.1-8b-instant",
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
        model="llama-3.1-8b-instant",
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
    """
    Safe fallback when the LLM call fails — build a minimal description from
    the spec fields directly, so the user still gets something useful.
    """
    trigger = workflow_spec.trigger
    action_parts = [
        f"{a.action_key} via {a.app_key}" for a in workflow_spec.actions
    ]
    actions_str = ", then ".join(action_parts) if action_parts else "perform configured actions"
    return (
        f"This workflow will monitor {trigger.app_key} for {trigger.trigger_key} events, "
        f"then {actions_str}."
    )
