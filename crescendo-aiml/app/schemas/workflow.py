"""
Workflow schemas
================
Pydantic models shared across all pipeline stages and the HTTP boundary.

Phase 3 additions (all additive / backward-compatible):
  - WorkflowEdge      — directed connection between two steps (for DAG workflows)
  - WorkflowSpec.edges — carries the explicit edge list (empty = linear chain)
  - ResolvedStep.client_id — step identifier used in edge references
  - IntentResult.has_branching / branch_description — conditional logic intent
  - WorkflowDraftRequest.session_id  — enables multi-turn continuation
  - WorkflowDraftResponse.session_id — echoed back so the client can resume
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any


# ---------------------------------------------------------------------------
# Sub-models: building blocks of a generated workflow
# ---------------------------------------------------------------------------

class TriggerNode(BaseModel):
    app_key: str            # exact catalog appKey  (e.g. "gmail")
    trigger_key: str        # exact triggerKey      (e.g. "gmail:new_email")
    config: Dict[str, Any] = {}   # filled by configurator stage


class ActionNode(BaseModel):
    app_key: str            # exact catalog appKey  (e.g. "slack")
    action_key: str         # exact actionKey       (e.g. "slack:post_message")
    config: Dict[str, Any] = {}   # filled by configurator stage


class WorkflowEdge(BaseModel):
    """
    A directed connection between two steps in a DAG workflow.

    source_handle is None for normal steps, "true"/"false" for logic:if branches,
    and the string of the outputIndex for logic:switch branches.

    step IDs match the client_id field on ResolvedStep (e.g. "step_0", "step_1").
    """
    source_step_id: str
    target_step_id: str
    source_handle: Optional[str] = None


class WorkflowSpec(BaseModel):
    workflow_name: str
    trigger: TriggerNode
    actions: List[ActionNode]
    description: str
    edges: List[WorkflowEdge] = []   # empty list = linear chain (backward compatible)


# ---------------------------------------------------------------------------
# Intermediate pipeline models (used by Stage 1 and Stage 2 agents)
# ---------------------------------------------------------------------------

class IntentResult(BaseModel):
    """Output of Stage 1 — intent classifier."""
    trigger_description: str
    action_descriptions: List[str]
    needs_clarification: bool
    clarifying_questions: List[str] = []
    # Phase 3 additions — branching intent
    has_branching: bool = False
    branch_description: Optional[str] = None


class ResolvedStep(BaseModel):
    """
    A single trigger or action step resolved against the real catalog.
    client_id is the stable identifier used in WorkflowEdge references.
    """
    app_key: str
    action_key: str          # triggerKey for the trigger step, actionKey for actions
    display_name: str
    config: Dict[str, Any] = {}
    client_id: str = ""      # e.g. "step_0", "step_1" — assigned by the resolver


# ---------------------------------------------------------------------------
# Request/Response — HTTP boundary
# ---------------------------------------------------------------------------

class WorkflowDraftRequest(BaseModel):
    userId: str = Field(..., description="UUID of the requesting user (forwarded by Java backend)")
    prompt: str = Field(..., min_length=1, max_length=8000, description="Natural-language workflow description")
    context: Dict[str, Any] = Field(default_factory=dict, description="Enriched context from AiContextService")
    session_id: Optional[str] = Field(None, description="Resume a previous conversation turn")


class WorkflowDraftResponse(BaseModel):
    workflow_spec: Optional[WorkflowSpec] = None
    explanation: Optional[str] = None
    clarifying_questions: List[str] = []
    error: Optional[str] = None
    success: bool
    session_id: Optional[str] = None   # echoed back so the client can resume