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
from typing import Any, Dict, List, Optional, Tuple
from langgraph.graph import END

from app.agents.client import get_groq_client
from app.agents.configurator import configure_steps
from app.agents.continuation import apply_workflow_update, is_refinement_prompt
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


def _unpack_lc(value: Any) -> Any:
    """
    Safely unwrap a value that may be a LangChain checkpoint dict.

    LangGraph v0.5+ serialises Pydantic objects in checkpoints as:
        {"lc": 1, "type": "constructor", "id": [...], "kwargs": {...}}
    When we read `state.get("intent")` on a resumed session the value may be
    this raw dict rather than the hydrated IntentResult.  This helper detects
    the sentinel and extracts the inner kwargs so callers can work with a plain
    dict regardless of whether the checkpoint was hydrated or not.
    """
    if isinstance(value, dict) and value.get("lc") == 1 and "kwargs" in value:
        return value["kwargs"]
    return value


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
# Node: continuation fast-path (focused edit on an existing session spec)
# ---------------------------------------------------------------------------

async def continuation_node(state: PipelineState) -> dict:
    """
    Fast-path for obvious refinement prompts (e.g. "also add Slack").
    Applies a bounded LLM edit directly to the prior workflow_spec without
    running the full intent → resolver → configurator chain.
    Falls back to returning the original spec if parsing fails.
    """
    prior_response = _unpack_lc(state.get("prior_final_response"))
    current_spec: Optional[WorkflowSpec] = None
    if isinstance(prior_response, dict):
        raw_spec = prior_response.get("workflow_spec")
        if isinstance(raw_spec, dict):
            try:
                current_spec = WorkflowSpec(**raw_spec)
            except Exception:
                current_spec = None
        elif isinstance(raw_spec, WorkflowSpec):
            current_spec = raw_spec
    elif prior_response is not None:
        current_spec = getattr(prior_response, "workflow_spec", None)

    if current_spec is None:
        # No prior spec in state — fall through to full pipeline via error
        return {
            "final_response": WorkflowDraftResponse(
                success=False,
                error="Cannot apply refinement: no prior workflow spec found in session.",
                session_id=state.get("session_id"),
            )
        }

    try:
        updated_spec = await apply_workflow_update(
            current_spec=current_spec,
            sanitized_prompt=state["sanitized_prompt"],
            user_id=state["user_id"],
            groq_client=get_groq_client(),
        )
    except Exception as exc:
        logger.error("continuation_node failed for user %s: %s", state["user_id"], exc)
        return {
            "final_response": WorkflowDraftResponse(
                success=False,
                error=f"Workflow update failed: {exc}",
                session_id=state.get("session_id"),
            )
        }

    explanation = await generate_explanation(updated_spec, state["user_id"], get_groq_client())
    response = WorkflowDraftResponse(
        success=True,
        workflow_spec=updated_spec,
        explanation=explanation,
        session_id=state.get("session_id"),
    )
    return {
        "workflow_spec": updated_spec,
        "explanation": explanation,
        "final_response": response,
        "prior_final_response": response,
    }


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
    # Extract multi-turn context from the persisted prior response (if any)
    prior_response = _unpack_lc(state.get("prior_final_response"))
    previous_questions: List[str] = []
    previous_spec = None
    if isinstance(prior_response, dict):
        previous_questions = prior_response.get("clarifying_questions") or []
        previous_spec = prior_response.get("workflow_spec")
    elif prior_response is not None:
        previous_questions = getattr(prior_response, "clarifying_questions", []) or []
        previous_spec = getattr(prior_response, "workflow_spec", None)

    previous_intent = _unpack_lc(state.get("intent"))

    try:
        intent = await classify_intent(
            state["sanitized_prompt"], state["user_id"], get_groq_client(),
            previous_questions=previous_questions,
            previous_spec=previous_spec,
            previous_intent=previous_intent,
        )
        return {
            "intent": intent,
            # Reset final_response so the stale clarify response from the prior
            # turn doesn't cause route_after_intent to terminate prematurely.
            "final_response": None,
        }
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
    suggested_options = []
    # Suggest only the user's active connected apps rather than random apps
    for conn in state.get("context", {}).get("connections", []):
        if conn.get("status") == "ACTIVE":
            app_key = conn.get("appKey")
            label = conn.get("label") or app_key
            if app_key:
                suggested_options.append({
                    "group": "Connected Apps",
                    "appKey": app_key,
                    "label": label,
                    "value": f"Use {label}"
                })
    response = WorkflowDraftResponse(
        success=True,
        workflow_spec=None,
        clarifying_questions=intent.clarifying_questions,
        suggested_options=suggested_options,
        session_id=state.get("session_id"),
    )
    return {
        "final_response": response,
        # Persist so intent_node can extract prior questions on the next turn.
        # Key must NOT start with _ (LangGraph serializer skips private-looking keys).
        "prior_final_response": response,
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

def _extract_dynamic_clarifications(
    trigger_step: ResolvedStep,
    action_steps: List[ResolvedStep],
    context: Dict[str, Any]
) -> Tuple[List[str], List[Dict[str, Any]]]:
    """
    Inspects steps for missing required configSchema fields and matches them
    generically against available resources in context["resources"] by exact resourceType.
    """
    from app.agents.validator import _build_catalog_index
    from app.catalog_sync import app_state

    catalog = app_state.get("catalog", [])
    if not catalog:
        return [], []

    index = _build_catalog_index(catalog)
    resources = context.get("resources", [])
    questions: List[str] = []
    options: List[Dict[str, Any]] = []

    steps_to_check = [(trigger_step, "triggers")] + [(s, "actions") for s in action_steps]

    for step, op_type in steps_to_check:
        app_entry = index.get(step.app_key, {})
        schema = app_entry.get(op_type, {}).get(step.action_key, [])
        for field in schema:
            if not isinstance(field, dict):
                continue
            if field.get("required") and field.get("type") not in ("section", "info"):
                field_key = field.get("key", "")
                val = step.config.get(field_key)
                if val is None or val == "":
                    field_label = field.get("label") or field_key
                    step_name = step.display_name or step.app_key
                    field_resource_type = field.get("resourceType")

                    # Check for live account resources matching this step's appKey AND resourceType
                    matching_blocks = [
                        r for r in resources
                        if r.get("appKey") == step.app_key
                        and (not field_resource_type or r.get("resourceType") == field_resource_type)
                        and r.get("items")
                    ]
                    if matching_blocks:
                        app_title = step.app_key.replace("-", " ").title()
                        group_name = f"{app_title}: {field_label}"
                        q = f"Which {field_label} in {app_title} should be used for {step_name}?"
                        if q not in questions:
                            questions.append(q)
                        for block in matching_blocks:
                            for item in block.get("items", []):
                                item_label = item.get("label") or item.get("id")
                                opt = {
                                    "group": group_name,
                                    "appKey": step.app_key,
                                    "fieldKey": field_key,
                                    "fieldLabel": field_label,
                                    "stepName": step_name,
                                    "label": f"{app_title}: {item_label}",
                                    "itemLabel": item_label,
                                    "value": f"Use {item_label} for {field_label} in {app_title}"
                                }
                                if opt not in options:
                                    options.append(opt)
    return questions, options


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

    structural_errors = (
        validate_workflow(spec)
        + validate_edges(trigger_step, action_steps, edges)
    )

    if structural_errors:
        if state.get("correction_attempted"):
            error_msg = f"Workflow validation failed after correction: {'; '.join(structural_errors[:3])}"
            logger.warning("Structural validation failed (post-correction) for user %s: %s", state["user_id"], structural_errors)
            return {
                "validation_errors": structural_errors,
                "final_response": WorkflowDraftResponse(
                    success=False,
                    error=error_msg,
                    session_id=state.get("session_id"),
                ),
            }
        logger.warning("Structural validation failed (will retry) for user %s: %s", state["user_id"], structural_errors)
        return {"validation_errors": structural_errors, "workflow_spec": spec}

    # Structural validation passed — return workflow spec so the user's workflow is drafted on canvas
    logger.info("Structural validation passed for user %s (ready for canvas)", state["user_id"])
    return {"workflow_spec": spec, "validation_errors": []}


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

    explanation = await generate_explanation(
        workflow_spec=spec,
        user_id=state["user_id"],
        groq_client=get_groq_client(),
        context=state.get("context"),
    )

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
    """After template_node: go to continuation on refinement hit, configurator on
    template hit, or intent on miss."""
    # Fast-path: focused edit on an existing checkpointed session
    if state.get("is_continuation") and is_refinement_prompt(state.get("sanitized_prompt", "")):
        # Only use the continuation path if a prior validated spec exists in state.
        prior = _unpack_lc(state.get("prior_final_response"))
        has_prior_spec = (
            (isinstance(prior, dict) and prior.get("workflow_spec") is not None)
            or (prior is not None and getattr(prior, "workflow_spec", None) is not None)
        )
        if has_prior_spec:
            return "continuation"

    if state.get("template_hit"):
        return "configurator"
    # Only route to 'end' on a genuine template error: final_response is set
    # AND template_hit is explicitly False (not just absent/None).
    # A normal cache-miss sets template_hit=False with no final_response.
    if state.get("final_response") and state.get("template_hit") is False:
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
