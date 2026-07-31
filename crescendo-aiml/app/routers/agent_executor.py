"""
Agent Executor Router
======================
Exposes:  POST /v1/agent/next-step

Called by Java's AgentExecutionService on every ReAct loop iteration.
Receives full context (system prompt + conversation history + tool definitions)
and returns exactly one decision: tool_call or final_answer.

Auth: same Bearer token scheme as workflow_builder.py — reads SERVICE_TOKEN
from env.  The token is set by Java in the HTTP call so Python never accepts
unauthenticated requests from outside the internal network.
"""

import logging
import os

from fastapi import APIRouter, Depends, HTTPException, Security, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.agents.react_agent import get_next_step
from app.schemas.agent import AgentNextStepRequest, AgentNextStepResponse

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/v1/agent", tags=["agent"])

# ---------------------------------------------------------------------------
# Auth — shared service token (Bearer)
# Same pattern as workflow_builder.py; duplicated intentionally to keep
# routers self-contained.  If a shared middleware module is added later,
# both routers can be migrated at once.
# ---------------------------------------------------------------------------

_bearer_scheme = HTTPBearer(auto_error=True)


def _verify_service_token(
    credentials: HTTPAuthorizationCredentials = Security(_bearer_scheme),
) -> None:
    """Validates the incoming Bearer token against SERVICE_TOKEN env var."""
    expected_token = os.getenv("SERVICE_TOKEN", "")
    if not expected_token:
        logger.error("SERVICE_TOKEN env var is not set; all agent requests are rejected.")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI service is misconfigured (missing SERVICE_TOKEN).",
        )
    if credentials.credentials != expected_token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid service token.",
            headers={"WWW-Authenticate": "Bearer"},
        )


# ---------------------------------------------------------------------------
# Route
# ---------------------------------------------------------------------------

@router.post(
    "/next-step",
    response_model=AgentNextStepResponse,
    summary="Get next ReAct decision for an agent turn",
)
async def agent_next_step(
    request: AgentNextStepRequest,
    _: None = Depends(_verify_service_token),
) -> AgentNextStepResponse:
    """
    Called by Java AgentExecutionService on every ReAct loop iteration.

    Stateless: Java sends the full conversation history each call.
    Python receives a complete context window and returns one decision.

    Returns:
    - `decision: "tool_call"` + `tool_call` object — Java dispatches the tool
      and appends the observation to conversation_history before the next call.
    - `decision: "final_answer"` + `final_answer` string — Java records the
      result and terminates the ReAct loop for this agent node.

    The loop is bounded by Java's token budget check in AgentExecutionService —
    if tokens_used accumulation exceeds AgentClusterConfig.tokenBudget(), Java
    stops calling this endpoint and records a budget-exceeded error.
    """
    logger.info(
        "Agent next-step: session=%s iteration=%d tools=%d history_turns=%d",
        request.session_id,
        request.iteration,
        len(request.tool_definitions),
        len(request.conversation_history),
    )
    return await get_next_step(request)
