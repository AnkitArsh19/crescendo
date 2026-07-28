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

    # ── fast-path flag ───────────────────────────────────────────────────────
    template_hit: bool           # True = skipped intent + resolver via template match

    # ── stage outputs ────────────────────────────────────────────────────────
    intent: Optional[IntentResult]
    trigger_step: Optional[ResolvedStep]
    action_steps: List[ResolvedStep]
    resolved_edges: List[WorkflowEdge]   # edges output by resolver or from template
    workflow_spec: Optional[WorkflowSpec]

    # ── validation ───────────────────────────────────────────────────────────
    validation_errors: List[str]
    correction_attempted: bool   # True after first correction retry

    # ── explanation ──────────────────────────────────────────────────────────
    explanation: Optional[str]

    # ── final packaged response ───────────────────────────────────────────────
    final_response: Optional[WorkflowDraftResponse]
