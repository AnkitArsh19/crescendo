"""
Workflow Planner — compatibility shim
======================================
The original plan_workflow() function is preserved here so that any code
still importing from this module continues to work during the transition.

All real orchestration has moved to:
  - app/agents/graph.py    — LangGraph StateGraph definition
  - app/agents/nodes.py    — per-stage node functions
  - app/routers/workflow_builder.py — invokes the graph directly

plan_workflow() is a synchronous wrapper around the async graph that
delegates to asyncio.run() — kept only for backward compatibility with
tests.  The production code path in the router uses graph.ainvoke() directly.
"""

import asyncio
import logging
from typing import Any, Dict
from uuid import uuid4

from app.schemas.workflow import WorkflowDraftResponse

logger = logging.getLogger(__name__)


def plan_workflow(
    prompt: str,
    context: Dict[str, Any],
    user_id: str,
) -> WorkflowDraftResponse:
    """
    Backward-compatible synchronous wrapper.
    Prefer calling the graph directly via the async router.
    """
    from app.agents.graph import get_graph
    from app.agents.state import PipelineState
    from app.security.sanitizer import sanitize_prompt

    sanitized = sanitize_prompt(prompt) if not prompt.startswith("<user_request>") else prompt
    session_id = str(uuid4())

    initial_state: PipelineState = {
        "sanitized_prompt": sanitized,
        "user_id": user_id,
        "context": context,
        "session_id": session_id,
        "template_hit": False,
        "intent": None,
        "trigger_step": None,
        "action_steps": [],
        "resolved_edges": [],
        "workflow_spec": None,
        "validation_errors": [],
        "correction_attempted": False,
        "explanation": None,
        "final_response": None,
    }

    graph = get_graph()
    config = {"configurable": {"thread_id": session_id}}

    try:
        final_state = asyncio.run(graph.ainvoke(initial_state, config=config))
        response = final_state.get("final_response")
        if response is None:
            return WorkflowDraftResponse(success=False, error="Pipeline produced no response")
        return response
    except Exception as exc:
        logger.exception("plan_workflow shim failed for user %s", user_id)
        return WorkflowDraftResponse(success=False, error=f"Pipeline error: {exc}")
