from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any


# ---------------------------------------------------------------------------
# Sub-models: building blocks of a generated workflow
# ---------------------------------------------------------------------------

class TriggerNode(BaseModel):
    app_key: str            # exact catalog appKey  (was: app_name)
    trigger_key: str        # exact triggerKey      (was: trigger_type)
    config: Dict[str, Any] = {}   # e.g. {"filter": "from:boss@company.com"}


class ActionNode(BaseModel):
    app_key: str            # exact catalog appKey  (was: app_name)
    action_key: str         # exact actionKey       (was: action_type)
    config: Dict[str, Any] = {}   # e.g. {"channel": "#general"}


class WorkflowSpec(BaseModel):
    workflow_name: str
    trigger: TriggerNode
    actions: List[ActionNode]
    description: str


# ---------------------------------------------------------------------------
# Intermediate pipeline models (used by Stage 1 and Stage 2 agents)
# ---------------------------------------------------------------------------

class IntentResult(BaseModel):
    """Output of Stage 1 — intent classifier."""
    trigger_description: str
    action_descriptions: List[str]
    needs_clarification: bool
    clarifying_questions: List[str] = []


class ResolvedStep(BaseModel):
    """A single trigger or action step resolved against the real catalog."""
    app_key: str
    action_key: str          # triggerKey for the trigger step, actionKey for actions
    display_name: str
    config: Dict[str, Any] = {}


# ---------------------------------------------------------------------------
# Request — matches exactly what the Java backend sends
# POST /v1/workflow-drafts
# {
#   "userId":  "uuid-string",
#   "prompt":  "NL text (max 8000 chars)",
#   "context": { ...optional key-value pairs... }
# }
# ---------------------------------------------------------------------------

class WorkflowDraftRequest(BaseModel):
    userId: str = Field(..., description="UUID of the requesting user (forwarded by Java backend)")
    prompt: str = Field(..., min_length=1, max_length=8000, description="Natural-language workflow description")
    context: Dict[str, Any] = Field(default_factory=dict, description="Optional context: connected apps, existing workflows, etc.")


# ---------------------------------------------------------------------------
# Response — returned to the Java backend (Map<String, Object>)
# ---------------------------------------------------------------------------

class WorkflowDraftResponse(BaseModel):
    workflow_spec: Optional[WorkflowSpec] = None
    explanation: Optional[str] = None           # NEW — plain English summary
    clarifying_questions: List[str] = []        # NEW — returned when needs_clarification=True
    error: Optional[str] = None
    success: bool