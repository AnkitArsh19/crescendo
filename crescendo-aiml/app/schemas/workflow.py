from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any


# ---------------------------------------------------------------------------
# Sub-models: building blocks of a generated workflow
# ---------------------------------------------------------------------------

class TriggerNode(BaseModel):
    app_name: str            # e.g. "Gmail"
    trigger_type: str        # e.g. "new_email"
    config: Dict[str, Any] = {}   # e.g. {"filter": "from:boss@company.com"}


class ActionNode(BaseModel):
    app_name: str            # e.g. "Slack"
    action_type: str         # e.g. "post_message"
    config: Dict[str, Any] = {}   # e.g. {"channel": "#general"}


class WorkflowSpec(BaseModel):
    workflow_name: str
    trigger: TriggerNode
    actions: List[ActionNode]
    description: str


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
    error: Optional[str] = None
    success: bool