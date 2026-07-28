"""
LangGraph Node Functions
========================
One async function per graph node.  Each function:
  1. Reads its inputs from PipelineState
  2. Calls the relevant stage function (pure logic in agents/)
  3. Returns a dict of state keys to update (LangGraph merge semantics)

Routing functions (conditional edges) are also defined here.
"""

import logging
from typing import Any, Dict, List, Optional
from langgraph.graph import END

from app.agents.client import get_groq_client
from app.agents.configurator import configure_steps
from app.agents.explainer import generate_explanation
from app.agents.intent import classify_intent
from app.agents.resolver import resolve_steps, resolve_steps_corrected
from app.agents.state import PipelineState
from app.agents.validator import (
    format_correction_context,
    validate_edges,
    validate_required_config,
    validate_workflow,
)
from app.schemas.workflow import (
    ActionNode,
    IntentResult,
    ResolvedStep,
    TriggerNode,
    WorkflowDraftResponse,
    WorkflowEdge,
    WorkflowSpec,
)
from app.templates.matcher import match_template

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Helper: build WorkflowSpec from resolved steps in state
# ---------------------------------------------------------------------------

def _build_spec(state: PipelineState) -> WorkflowSpec:
    trigger_step: ResolvedStep = state["trigger_step"]
    action_steps: List[ResolvedStep] = state.get("action_steps", [])
    edges: List[WorkflowEdge] = state.get("resolved_edges", [])
    intent: Optional[IntentResult] = state.get("intent")

    if intent:
        # Use intent descriptions for a readable workflow name
        trigger_label = intent.trigger_description[:60]
        action_labels = [d[:30] for d in intent.action_descriptions[:3]]
        workflow_name = f"{trigger_label} → {' → '.join(action_labels)}"[:100]
        description = intent.trigger_description
    else:
        workflow_name = (
            f"{trigger_step.app_key} → "
            + " → ".join(s.app_key for s in action_steps)
        )
        description = workflow_name

    return WorkflowSpec(
        workflow_name=workflow_name,
        description=description,
        trigger=TriggerNode(
            app_key=trigger_step.app_key,
            trigger_key=trigger_step.action_key,
            config=trigger_step.config,
        ),
        actions=[
            ActionNode(
                app_key=s.app_key,
                action_key=s.action_key,
                config=s.config,
            )
            for s in action_steps
        ],
        edges=edges,
    )


def _clean_prompt(sanitized_prompt: str) -> str:
    """Strip the <user_request>…</user_request> delimiters for display."""
    return (
        sanitized_prompt
        .replace("<user_request>", "")
        .replace("</user_request>", "")
        .strip()
    )


# ---------------------------------------------------------------------------
# Node: template fast-path
# ---------------------------------------------------------------------------

async def template_node(state: PipelineState) -> dict:
    """
    Try to match the prompt against the pre-built template library.
    On hit: populates trigger_step and action_steps so configurator can run.
    On miss: returns template_hit=False and the graph routes to intent_node.
    """
    try:
        matched = await match_template(state["sanitized_prompt"], get_groq_client())
    except Exception as exc:
        logger.warning("Template node failed: %s — falling through to full pipeline", exc)
        return {"template_hit": False}

    if matched is None:
        return {"template_hit": False}

    spec = matched.spec
    trigger_step = ResolvedStep(
        app_key=spec.trigger.app_key,
        action_key=spec.trigger.trigger_key,
        display_name=f"{spec.trigger.app_key} {spec.trigger.trigger_key}",
        client_id="step_0",
    )
    action_steps = [
        ResolvedStep(
            app_key=a.app_key,
            action_key=a.action_key,
            display_name=f"{a.app_key} {a.action_key}",
            client_id=f"step_{i + 1}",
        )
        for i, a in enumerate(spec.actions)
    ]
    # Synthetic intent so configurator has some context
    synthetic_intent = IntentResult(
        trigger_description=f"Trigger: {spec.trigger.app_key} {spec.trigger.trigger_key}",
        action_descriptions=[f"{a.app_key} {a.action_key}" for a in spec.actions],
        needs_clarification=False,
    )
    return {
        "template_hit": True,
        "trigger_step": trigger_step,
        "action_steps": action_steps,
        "resolved_edges": list(spec.edges),
        "intent": synthetic_intent,
    }


# ---------------------------------------------------------------------------
# Node: intent classification
# ---------------------------------------------------------------------------

async def intent_node(state: PipelineState) -> dict:
    try:
        intent = await classify_intent(
            state["sanitized_prompt"], state["user_id"], get_groq_client()
        )
        return {"intent": intent}
    except Exception as exc:
        logger.error("intent_node failed for user %s: %s", state["user_id"], exc)
        return {
            "final_response": WorkflowDraftResponse(
                success=False,
                error=f"Intent classification failed: {exc}",
                session_id=state.get("session_id"),
            )
        }


# ---------------------------------------------------------------------------
# Node: clarify (short-circuit when needs_clarification=True)
# ---------------------------------------------------------------------------

async def clarify_node(state: PipelineState) -> dict:
    intent: IntentResult = state["intent"]
    return {
        "final_response": WorkflowDraftResponse(
            success=True,
            clarifying_questions=intent.clarifying_questions,
            session_id=state.get("session_id"),
        )
    }


# ---------------------------------------------------------------------------
# Node: resolver
# ---------------------------------------------------------------------------

async def resolver_node(state: PipelineState) -> dict:
    try:
        trigger_step, action_steps, edges = await resolve_steps(
            state["intent"], state["user_id"], get_groq_client()
        )
        return {
            "trigger_step": trigger_step,
            "action_steps": action_steps,
            "resolved_edges": edges,
        }
    except Exception as exc:
        logger.error("resolver_node failed for user %s: %s", state["user_id"], exc)
        return {
            "final_response": WorkflowDraftResponse(
                success=False,
                error=f"App resolution failed: {exc}",
                session_id=state.get("session_id"),
            )
        }


# ---------------------------------------------------------------------------
# Node: configurator
# ---------------------------------------------------------------------------

async def configurator_node(state: PipelineState) -> dict:
    trigger_step: Optional[ResolvedStep] = state.get("trigger_step")
    action_steps: List[ResolvedStep] = state.get("action_steps", [])

    if trigger_step is None:
        return {}   # safety guard — resolver should have caught this

    try:
        updated_trigger, updated_actions = await configure_steps(
            trigger_step=trigger_step,
            action_steps=action_steps,
            context=state.get("context", {}),
            user_prompt=_clean_prompt(state["sanitized_prompt"]),
            user_id=state["user_id"],
            groq_client=get_groq_client(),
        )
        return {"trigger_step": updated_trigger, "action_steps": updated_actions}
    except Exception as exc:
        logger.warning("configurator_node failed gracefully for user %s: %s", state["user_id"], exc)
        return {}   # config stays empty — validator will flag missing required fields


# ---------------------------------------------------------------------------
# Node: validator
# ---------------------------------------------------------------------------

async def validator_node(state: PipelineState) -> dict:
    trigger_step: Optional[ResolvedStep] = state.get("trigger_step")
    action_steps: List[ResolvedStep] = state.get("action_steps", [])
    edges: List[WorkflowEdge] = state.get("resolved_edges", [])

    if trigger_step is None:
        return {
            "validation_errors": ["No trigger step was resolved"],
            "final_response": WorkflowDraftResponse(
                success=False,
                error="No trigger step was resolved",
                session_id=state.get("session_id"),
            ),
        }

    spec = _build_spec(state)

    errors = (
        validate_workflow(spec)
        + validate_required_config(trigger_step, action_steps)
        + validate_edges(trigger_step, action_steps, edges)
    )

    if not errors:
        logger.info("Validation passed for user %s", state["user_id"])
        return {"workflow_spec": spec, "validation_errors": []}

    # Errors found
    if state.get("correction_attempted"):
        # Second failure — surface error to user
        error_msg = f"Workflow validation failed after correction: {'; '.join(errors[:3])}"
        logger.warning("Validation failed (post-correction) for user %s: %s", state["user_id"], errors)
        return {
            "validation_errors": errors,
            "final_response": WorkflowDraftResponse(
                success=False,
                error=error_msg,
                session_id=state.get("session_id"),
            ),
        }

    logger.warning("Validation failed (will retry) for user %s: %s", state["user_id"], errors)
    return {"validation_errors": errors, "workflow_spec": spec}


# ---------------------------------------------------------------------------
# Node: correction retry
# ---------------------------------------------------------------------------

async def correction_node(state: PipelineState) -> dict:
    intent: Optional[IntentResult] = state.get("intent")
    errors: List[str] = state.get("validation_errors", [])

    if intent is None:
        return {"correction_attempted": True}

    try:
        trigger_step, action_steps, edges = await resolve_steps_corrected(
            intent=intent,
            validation_errors=errors,
            user_id=state["user_id"],
            groq_client=get_groq_client(),
        )
        # Re-run configurator on the corrected steps
        updated_trigger, updated_actions = await configure_steps(
            trigger_step=trigger_step,
            action_steps=action_steps,
            context=state.get("context", {}),
            user_prompt=_clean_prompt(state["sanitized_prompt"]),
            user_id=state["user_id"],
            groq_client=get_groq_client(),
        )
        return {
            "trigger_step": updated_trigger,
            "action_steps": updated_actions,
            "resolved_edges": edges,
            "correction_attempted": True,
        }
    except Exception as exc:
        logger.error("correction_node failed for user %s: %s", state["user_id"], exc)
        return {"correction_attempted": True}   # validator will surface the error


# ---------------------------------------------------------------------------
# Node: explainer
# ---------------------------------------------------------------------------

async def explainer_node(state: PipelineState) -> dict:
    spec: Optional[WorkflowSpec] = state.get("workflow_spec")
    if spec is None:
        spec = _build_spec(state)

    explanation = await generate_explanation(spec, state["user_id"], get_groq_client())

    return {
        "explanation": explanation,
        "final_response": WorkflowDraftResponse(
            success=True,
            workflow_spec=spec,
            explanation=explanation,
            session_id=state.get("session_id"),
        ),
    }


# ---------------------------------------------------------------------------
# Routing functions (conditional edges)
# ---------------------------------------------------------------------------

def route_template(state: PipelineState) -> str:
    """After template_node: go to configurator on hit, intent on miss."""
    if state.get("template_hit"):
        return "configurator"
    if state.get("final_response"):   # template_node error
        return "end"
    return "intent"


def route_after_intent(state: PipelineState) -> str:
    """After intent_node: clarify if needed, else resolve."""
    if state.get("final_response"):   # intent error
        return "end"
    intent: Optional[IntentResult] = state.get("intent")
    if intent and intent.needs_clarification:
        return "clarify"
    return "resolver"


def route_after_resolver(state: PipelineState) -> str:
    """After resolver_node: go to configurator unless an error was set."""
    if state.get("final_response"):
        return "end"
    return "configurator"


def route_after_validator(state: PipelineState) -> str:
    """After validator_node: correct, explain, or surface error."""
    if state.get("final_response"):   # error already packaged
        return "end"
    if state.get("validation_errors"):
        if state.get("correction_attempted"):
            return "end"       # correction already tried; final_response was set in validator_node
        return "correction"
    return "explainer"
