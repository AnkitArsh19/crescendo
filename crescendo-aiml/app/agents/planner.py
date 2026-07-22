"""
Workflow Planner — Pipeline Orchestrator
=========================================
Replaces the previous single-shot LLM call with a 5-stage pipeline:

  Stage 1 — Intent classifier  (llama-3.1-8b-instant)
      ↓  (short-circuit if needs_clarification)
  Stage 2 — App + action resolver  (llama-3.3-70b-versatile, catalog-filtered)
      ↓
  [Stage 3 — Config inference — deferred; resolver populates config for now]
      ↓
  Stage 4 — Catalog validator  (pure Python, no LLM)
      ↓  (on failure → one correction retry via resolver → if still fails → error)
  Stage 5 — Explanation generator  (llama-3.1-8b-instant)
      ↓
  WorkflowDraftResponse

All user input is sanitized before Stage 1.
All stages emit audit log records.
"""

import json
import logging
import os
from typing import Any, Dict, Optional

from groq import Groq

from app.schemas.workflow import (
    ActionNode,
    TriggerNode,
    WorkflowDraftResponse,
    WorkflowSpec,
)
from app.agents.intent import classify_intent
from app.agents.resolver import resolve_steps, _build_resolver_prompt, _filter_catalog
from app.agents.validator import validate_workflow, format_correction_context
from app.agents.explainer import generate_explanation
from app.audit.logger import audit_log
from app.catalog_sync import app_state

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

def _build_groq_client() -> Optional[Groq]:
    """Return a configured Groq client, or None if key is missing."""
    api_key = os.getenv("GROQ_API_KEY", "")
    if not api_key:
        return None
    return Groq(api_key=api_key)


def _assembled_spec(
    trigger_step,
    action_steps,
    intent,
) -> WorkflowSpec:
    """Convert resolved steps into a full WorkflowSpec."""
    trigger_node = TriggerNode(
        app_key=trigger_step.app_key,
        trigger_key=trigger_step.action_key,   # action_key on trigger step is the triggerKey
        config=trigger_step.config,
    )
    action_nodes = [
        ActionNode(
            app_key=s.app_key,
            action_key=s.action_key,
            config=s.config,
        )
        for s in action_steps
    ]
    name_parts = [intent.trigger_description[:40]] + [d[:25] for d in intent.action_descriptions[:2]]
    workflow_name = " → ".join(name_parts)[:80]

    return WorkflowSpec(
        workflow_name=workflow_name,
        trigger=trigger_node,
        actions=action_nodes,
        description=intent.trigger_description,
    )


def _correction_retry(
    intent,
    validation_errors: list[str],
    user_id: str,
    groq_client: Groq,
) -> tuple:
    """
    One correction attempt: re-run Stage 2 with the validation errors injected
    into the prompt so the model can pick correct catalog keys.
    """
    catalog = app_state.get("catalog", [])
    filtered = _filter_catalog(intent, catalog)
    correction_note = format_correction_context(validation_errors)

    # Build an augmented resolver prompt that includes the errors
    base_system = _build_resolver_prompt(intent, filtered)
    correction_system = base_system + "\n\n" + correction_note

    try:
        response = groq_client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": correction_system},
                {"role": "user",   "content": "Correct the keys and return valid JSON."},
            ],
            temperature=0.1,
            max_tokens=1024,
            response_format={"type": "json_object"},
        )
    except Exception as exc:
        logger.exception("Correction retry LLM call failed for user %s", user_id)
        raise RuntimeError(f"Correction retry LLM error: {exc}") from exc

    usage = response.usage
    audit_log(
        user_id=user_id,
        stage="correction",
        model="llama-3.3-70b-versatile",
        prompt_tokens=usage.prompt_tokens if usage else 0,
        completion_tokens=usage.completion_tokens if usage else 0,
        validation_passed=False,
        error="; ".join(validation_errors),
    )

    raw = response.choices[0].message.content or ""
    data = json.loads(raw)

    from app.schemas.workflow import ResolvedStep
    trigger_step = ResolvedStep(**data["trigger"])
    action_steps = [ResolvedStep(**a) for a in data.get("actions", [])]
    return trigger_step, action_steps


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def plan_workflow(
    prompt: str,
    context: Dict[str, Any],
    user_id: str,
) -> WorkflowDraftResponse:
    """
    Orchestrate the full 5-stage pipeline.

    The `prompt` must already be sanitized by `sanitizer.sanitize_prompt` before
    being passed here (the router handles this).  `context` carries optional hints
    forwarded by the Java backend.

    Returns a WorkflowDraftResponse with success=False + error on any failure,
    so callers always receive a safe result.
    """
    groq_client = _build_groq_client()
    if groq_client is None:
        logger.error("GROQ_API_KEY is not set")
        return WorkflowDraftResponse(
            success=False,
            error="AI service is not configured (missing API key).",
        )

    # ------------------------------------------------------------------
    # Stage 1 — Intent classification
    # ------------------------------------------------------------------
    try:
        intent = classify_intent(
            sanitized_prompt=prompt,
            user_id=user_id,
            groq_client=groq_client,
        )
    except RuntimeError as exc:
        return WorkflowDraftResponse(success=False, error=str(exc))

    logger.info(
        "Stage 1 complete: trigger=%r, actions=%r, needs_clarification=%s",
        intent.trigger_description,
        intent.action_descriptions,
        intent.needs_clarification,
    )

    # Short-circuit: return clarifying questions immediately
    if intent.needs_clarification:
        logger.info("Needs clarification — returning questions to user %s", user_id)
        return WorkflowDraftResponse(
            success=True,
            clarifying_questions=intent.clarifying_questions,
        )

    # ------------------------------------------------------------------
    # Stage 2 — App + action resolution
    # ------------------------------------------------------------------
    try:
        trigger_step, action_steps = resolve_steps(
            intent=intent,
            user_id=user_id,
            groq_client=groq_client,
        )
    except RuntimeError as exc:
        return WorkflowDraftResponse(success=False, error=str(exc))

    logger.info(
        "Stage 2 complete: trigger=%s/%s, actions=%s",
        trigger_step.app_key,
        trigger_step.action_key,
        [(a.app_key, a.action_key) for a in action_steps],
    )

    # Assemble WorkflowSpec
    workflow_spec = _assembled_spec(trigger_step, action_steps, intent)

    # ------------------------------------------------------------------
    # Stage 4 — Catalog validation (+ one correction retry on failure)
    # ------------------------------------------------------------------
    validation_errors = validate_workflow(workflow_spec)

    if validation_errors:
        logger.warning(
            "Stage 4: validation failed (%d errors) — attempting correction retry for user %s",
            len(validation_errors),
            user_id,
        )
        try:
            trigger_step, action_steps = _correction_retry(
                intent=intent,
                validation_errors=validation_errors,
                user_id=user_id,
                groq_client=groq_client,
            )
            workflow_spec = _assembled_spec(trigger_step, action_steps, intent)
        except (RuntimeError, Exception) as exc:
            logger.error("Correction retry failed for user %s: %s", user_id, exc)
            return WorkflowDraftResponse(
                success=False,
                error=(
                    f"Workflow generation failed catalog validation and could not be "
                    f"automatically corrected. Errors: {'; '.join(validation_errors)}"
                ),
            )

        # Validate the corrected spec
        second_errors = validate_workflow(workflow_spec)
        if second_errors:
            audit_log(
                user_id=user_id,
                stage="correction",
                model="llama-3.3-70b-versatile",
                prompt_tokens=0,
                completion_tokens=0,
                validation_passed=False,
                error="; ".join(second_errors),
            )
            return WorkflowDraftResponse(
                success=False,
                error=(
                    f"Workflow generation failed catalog validation after correction. "
                    f"Errors: {'; '.join(second_errors)}"
                ),
            )

        logger.info("Correction retry succeeded for user %s", user_id)

    # Log successful validation
    audit_log(
        user_id=user_id,
        stage="validator",
        model="none",
        prompt_tokens=0,
        completion_tokens=0,
        validation_passed=True,
        error=None,
    )

    # ------------------------------------------------------------------
    # Stage 5 — Explanation
    # ------------------------------------------------------------------
    explanation = generate_explanation(
        workflow_spec=workflow_spec,
        user_id=user_id,
        groq_client=groq_client,
    )

    logger.info(
        "Pipeline complete for user %s: workflow=%r explanation=%r",
        user_id,
        workflow_spec.workflow_name,
        explanation[:80],
    )

    return WorkflowDraftResponse(
        success=True,
        workflow_spec=workflow_spec,
        explanation=explanation,
    )
