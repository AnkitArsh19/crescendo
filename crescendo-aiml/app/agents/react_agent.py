"""
ReAct Agent — Single-Turn LLM Oracle
======================================
Implements one ReAct loop iteration: receives the full conversation context
from Java and returns exactly one decision (tool_call or final_answer).

Design decisions:
  - Uses llama-3.1-70b-versatile (not 8b-instant) for reliable function-calling
    argument generation.  The 8b model frequently halluccinates or omits
    required parameters, which surfaces as tool errors in the Java loop.
  - tool_choice="auto" lets the LLM decide when to call a tool vs. answer
    directly.  "required" would force a tool call every turn — incorrect for
    the general case where the agent might have gathered enough info to answer.
  - If tool_definitions is empty, no tools are added to the Groq call and the
    LLM always falls through to final_answer (graceful degradation for text-
    only agent nodes).
  - Phase 2 message format: assistant turns that contained a tool call are
    reconstructed with a tool_calls array so that subsequent "tool" role
    observations can reference the correct tool_call_id.  Without this, Groq
    returns a 400 on the second iteration of any multi-tool run.
"""

import json
import logging
from typing import List, Optional, Tuple

from app.agents.client import get_groq_client
from app.audit.logger import audit_log
from app.schemas.agent import (
    AgentNextStepRequest,
    AgentNextStepResponse,
    ConversationTurn,
    ToolCall,
    ToolDefinition,
)

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _to_groq_tool(tool_def: ToolDefinition) -> dict:
    """Convert a ToolDefinition into Groq's function-calling schema."""
    return {
        "type": "function",
        "function": {
            "name": tool_def.tool_id,
            "description": tool_def.description,
            "parameters": tool_def.parameters or {
                "type": "object",
                "properties": {},
                "required": [],
            },
        },
    }


def _resolve_tool_fields(
    tool_id: str,
    tool_defs: List[ToolDefinition],
) -> Tuple[str, str]:
    """Look up app_key and action_key from tool_definitions by tool_id."""
    for t in tool_defs:
        if t.tool_id == tool_id:
            return t.app_key, t.action_key
    logger.warning("tool_id '%s' not found in tool_definitions — returning unknown", tool_id)
    return "unknown", tool_id


def _build_messages(
    system_prompt: str,
    history: List[ConversationTurn],
    input_data: dict,
) -> list:
    """
    Build the messages list for the Groq API call.

    Phase 2 multi-turn format:
      - If an assistant turn has a tool_call_id AND tool_name, it is
        reconstructed as an assistant message with a tool_calls array.
        This is what Groq expects when the assistant previously called a tool.
      - Tool observation turns (role == "tool") must have tool_call_id set
        to the id from the preceding assistant message — Java is responsible
        for propagating this correctly.
      - If history is empty, the input_data is injected as the first user message
        so the agent has context to reason about on the first iteration.
    """
    messages = [{"role": "system", "content": system_prompt}]

    if not history:
        # First iteration — seed the conversation with trigger/input data
        messages.append({
            "role": "user",
            "content": json.dumps(input_data, ensure_ascii=False),
        })
        return messages

    # Guard: if history exists but doesn't start with a user turn, inject
    # input_data first so the message sequence is always system → user → ...
    # This handles the edge case where Java sends only assistant+tool turns.
    if history and history[0].role != "user":
        messages.append({
            "role": "user",
            "content": json.dumps(input_data, ensure_ascii=False),
        })

    for turn in history:
        if turn.role == "assistant" and turn.tool_call_id and turn.tool_name:
            # Phase 2: reconstruct assistant message with tool_calls array
            # Required for Groq to accept the subsequent "tool" observation message
            messages.append({
                "role": "assistant",
                "content": turn.content or "",
                "tool_calls": [
                    {
                        "id": turn.tool_call_id,
                        "type": "function",
                        "function": {
                            "name": turn.tool_name,
                            "arguments": turn.tool_args_json or "{}",
                        },
                    }
                ],
            })
        elif turn.role == "tool":
            # Tool observation — must reference the assistant's tool_call_id
            messages.append({
                "role": "tool",
                "tool_call_id": turn.tool_call_id or "",
                "content": turn.content,
            })
        else:
            # Regular user or assistant message
            messages.append({"role": turn.role, "content": turn.content or ""})

    return messages


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

async def get_next_step(request: AgentNextStepRequest) -> AgentNextStepResponse:
    """
    Single ReAct turn: send conversation history + tool definitions to Groq,
    parse the typed decision, return to Java.

    Returns one of:
      - AgentNextStepResponse(decision="tool_call",    tool_call=...,      tokens_used=N)
      - AgentNextStepResponse(decision="final_answer", final_answer=...,   tokens_used=N)

    Never raises — errors surface as final_answer responses with an error
    message so the Java loop can record them and terminate gracefully.
    """
    client = get_groq_client()
    tools = [_to_groq_tool(t) for t in request.tool_definitions]
    messages = _build_messages(
        request.system_prompt,
        request.conversation_history,
        request.input_data,
    )

    kwargs: dict = {
        "model": "llama-3.3-70b-versatile",  # 3.3 > 3.1 for function-calling; 3.1-70b is deprecated
        "messages": messages,
        "max_tokens": 1024,
        "temperature": 0.2,
    }

    # Only attach tools if the agent node has toolRefs configured.
    # An empty tool list causes Groq to always produce a text completion
    # (final_answer), which is the correct behaviour for a reasoning-only
    # agent node with no connected action steps.
    if tools:
        kwargs["tools"] = tools
        kwargs["tool_choice"] = "auto"

    try:
        response = await client.chat.completions.create(**kwargs)
    except Exception as exc:
        logger.exception(
            "Groq call failed for session=%s iter=%d",
            request.session_id,
            request.iteration,
        )
        return AgentNextStepResponse(
            decision="final_answer",
            final_answer=f"[Agent error: LLM call failed — {exc}]",
            tokens_used=0,
        )

    choice = response.choices[0]
    usage = response.usage
    tokens_used = usage.total_tokens if usage else 0

    # Audit log — every agent turn tracked the same as other pipeline stages
    audit_log(
        user_id=request.session_id,   # session_id is the agent-run identifier
        stage=f"agent_iter_{request.iteration}",
        model="llama-3.3-70b-versatile",
        prompt_tokens=usage.prompt_tokens if usage else 0,
        completion_tokens=usage.completion_tokens if usage else 0,
        validation_passed=True,
        error=None,
    )

    # ── Tool call decision ──────────────────────────────────────────────────
    if choice.finish_reason == "tool_calls" and choice.message.tool_calls:
        tc = choice.message.tool_calls[0]

        try:
            arguments = json.loads(tc.function.arguments)
        except (json.JSONDecodeError, TypeError):
            # Groq occasionally returns malformed JSON from smaller models.
            # An empty dict surfaces as a tool error observation in Java —
            # the agent loop handles it and can retry or give up gracefully.
            logger.warning(
                "session=%s iter=%d — could not parse tool arguments JSON: %r",
                request.session_id,
                request.iteration,
                tc.function.arguments,
            )
            arguments = {}

        app_key, action_key = _resolve_tool_fields(
            tc.function.name, request.tool_definitions
        )

        logger.info(
            "session=%s iter=%d → tool_call  tool_id=%s app=%s action=%s tokens=%d",
            request.session_id,
            request.iteration,
            tc.function.name,
            app_key,
            action_key,
            tokens_used,
        )

        return AgentNextStepResponse(
            decision="tool_call",
            tool_call=ToolCall(
                tool_id=tc.function.name,
                app_key=app_key,
                action_key=action_key,
                arguments=arguments,
            ),
            reasoning=choice.message.content,  # chain-of-thought before the tool call
            tokens_used=tokens_used,
        )

    # ── Final answer decision ───────────────────────────────────────────────
    final = choice.message.content or ""
    logger.info(
        "session=%s iter=%d → final_answer  length=%d tokens=%d",
        request.session_id,
        request.iteration,
        len(final),
        tokens_used,
    )
    return AgentNextStepResponse(
        decision="final_answer",
        final_answer=final,
        tokens_used=tokens_used,
    )
