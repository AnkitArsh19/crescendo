"""
Workflow Builder Router
=======================
Exposes:  POST /v1/workflow-drafts

Phase 3 changes:
  - Calls the LangGraph graph directly (async, non-blocking)
  - Accepts and echoes session_id for multi-turn continuation
  - Generates a new session_id if the caller doesn't provide one
"""

import logging
import os
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, Request, Security, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.agents.graph import get_graph
from app.agents.state import PipelineState
from app.schemas.workflow import WorkflowDraftRequest, WorkflowDraftResponse
from app.security.sanitizer import sanitize_prompt

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/v1", tags=["workflow-drafts"])

# ---------------------------------------------------------------------------
# Auth — shared service token (Bearer)
# ---------------------------------------------------------------------------

_bearer_scheme = HTTPBearer(auto_error=True)


def _verify_service_token(
    credentials: HTTPAuthorizationCredentials = Security(_bearer_scheme),
) -> None:
    """Validates the incoming Bearer token against SERVICE_TOKEN env var."""
    expected_token = os.getenv("SERVICE_TOKEN", "")
    if not expected_token:
        logger.error("SERVICE_TOKEN env var is not set; all requests are rejected.")
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
    "/workflow-drafts",
    response_model=WorkflowDraftResponse,
    summary="Generate a workflow draft from a natural-language prompt",
)
async def create_workflow_draft(
    body: WorkflowDraftRequest,
    _: None = Depends(_verify_service_token),
) -> WorkflowDraftResponse:
    """
    Accepts a natural-language prompt and returns a structured WorkflowSpec.

    Multi-turn usage:
      - First call: omit session_id (or pass null). A new one is generated and returned.
      - Subsequent turns: pass the returned session_id to resume conversation state.

    The context field receives the enriched payload from AiContextService:
      - connections[]  — user's connected app accounts (real UUIDs)
      - resources[]    — live resource lists (Slack channels, sheets, etc.)
    """
    logger.info("Workflow draft request: user=%s session=%s", body.userId, body.session_id)

    # Sanitize and wrap user input
    sanitized_prompt = sanitize_prompt(body.prompt)

    # Session management
    session_id = body.session_id or str(uuid4())

    if body.session_id:
        initial_state: PipelineState = {
            "sanitized_prompt": sanitized_prompt,
            "user_id": body.userId,
            "context": body.context,
            "session_id": session_id,
            "is_continuation": True,
            "template_hit": False,
            "validation_errors": [],
            "correction_attempted": False,
            "final_response": None,
        }
    else:
        initial_state: PipelineState = {
            "sanitized_prompt": sanitized_prompt,
            "user_id": body.userId,
            "context": body.context,
            "session_id": session_id,
            "is_continuation": False,
            "template_hit": False,
            "intent": None,
            "trigger_step": None,
            "action_steps": [],
            "resolved_edges": [],
            "validation_errors": [],
            "correction_attempted": False,
            "final_response": None,
        }

    graph = get_graph()
    config = {"configurable": {"thread_id": session_id}}

    try:
        final_state = await graph.ainvoke(initial_state, config=config)
    except Exception as exc:
        logger.exception("Graph invocation failed for user %s", body.userId)
        return WorkflowDraftResponse(
            success=False,
            error=f"Pipeline error: {exc}",
            session_id=session_id,
        )

    response: WorkflowDraftResponse = final_state.get("final_response") or WorkflowDraftResponse(
        success=False,
        error="Pipeline produced no response",
        session_id=session_id,
    )

    # Always echo session_id so the client can continue the conversation
    if response.session_id is None:
        response = response.model_copy(update={"session_id": session_id})

    logger.info(
        "Workflow draft complete: user=%s session=%s success=%s",
        body.userId, session_id, response.success,
    )
    return response
