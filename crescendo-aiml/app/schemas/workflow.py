from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class TriggerNode(BaseModel):
    app_name: str           # e.g. "Gmail"
    trigger_type: str       # e.g. "new_email"
    config: Dict[str, Any] = {}  # e.g. {"filter": "from:boss@company.com"}

class ActionNode(BaseModel):
    app_name: str           # e.g. "Slack"
    action_type: str        # e.g. "post_message"
    config: Dict[str, Any] = {}  # e.g. {"channel": "#general"}

class WorkflowSpec(BaseModel):
    workflow_name: str
    trigger: TriggerNode
    actions: List[ActionNode]
    description: str

class NLWorkflowRequest(BaseModel):
    user_input: str         # "When I get an email, post to Slack"

class NLWorkflowResponse(BaseModel):
    workflow_spec: Optional[WorkflowSpec] = None
    error: Optional[str] = None
    success: bool