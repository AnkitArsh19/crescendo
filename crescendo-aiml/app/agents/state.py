"""
PipelineState — shared LangGraph state TypedDict
=================================================
Every node reads from and writes partial updates to this state.
LangGraph merges returned dicts into the state; unmentioned keys stay unchanged.
"""

from typing import Any, Dict, List, Optional
from typing_extensions import TypedDict

from app.schemas.workflow import (
    IntentResult,
    ResolvedStep,
    WorkflowDraftResponse,
    WorkflowEdge,
    WorkflowSpec,
)


class PipelineState(TypedDict, total=False):
    # ── inputs ──────────────────────────────────────────────────────────────
    sanitized_prompt: str        # XML-wrapped user prompt from sanitizer
    user_id: str
    context: Dict[str, Any]      # enriched context from AiContextService
    session_id: str
    is_continuation: bool     # True only when caller supplied an existing session_id

    # ── fast-path flag ───────────────────────────────────────────────────────
    template_hit: bool           # True = skipped intent + resolver via template match

    # ── stage outputs ────────────────────────────────────────────────────────
    intent: Optional[IntentResult]
    trigger_step: Optional[ResolvedStep]
    action_steps: List[ResolvedStep]
    resolved_edges: List[WorkflowEdge]   # edges output by resolver or from template
    workflow_spec: Optional[WorkflowSpec]
    continuation_spec: Optional[WorkflowSpec]  # focused edit, validated without rebuilding it

    # ── validation ───────────────────────────────────────────────────────────
    validation_errors: List[str]
    correction_attempted: bool   # True after first correction retry

    # ── explanation ──────────────────────────────────────────────────────────
    explanation: Optional[str]

    # ── final packaged response ───────────────────────────────────────────────
    final_response: Optional[WorkflowDraftResponse]

    # ── multi-turn persistence ────────────────────────────────────────────────
    # Stores the last clarification response across turns so intent_node can
    # extract prior questions and intent context from the checkpoint on resume.
    # NOTE: NOT prefixed with _ — LangGraph's serializer skips private-looking keys.
    prior_final_response: Optional[WorkflowDraftResponse]
