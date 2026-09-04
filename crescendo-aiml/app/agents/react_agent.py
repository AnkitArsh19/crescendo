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
from typing import Dict, List, Optional, Tuple

from app.agents.client import get_groq_client
from app.audit.logger import audit_log
from app.schemas.agent import (
    AgentNextStepRequest,
    AgentNextStepResponse,
    ConversationTurn,
    ToolCall,
    ToolDefinition,
)

from app.agents.models import REASONING_MODEL

logger = logging.getLogger(__name__)

# Maximum number of (assistant tool-call + tool observation) pairs to keep in
# the sliding window before pruning the oldest ones.
# With 10-iter cap and typical 600-token tool outputs, 5 pairs caps context
# growth and prevents Groq 8k-limit errors on long runs.
_WINDOW_MAX_TOOL_PAIRS = 5

# Default model — configurable per request via AgentNextStepRequest.model
_DEFAULT_MODEL = REASONING_MODEL


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
    Build the messages list for the Groq API call with sliding-window pruning.

    Window strategy:
      - system message and the first user message (input_data) are ALWAYS kept.
      - Regular user/assistant turns are kept in full.
      - When there are more than _WINDOW_MAX_TOOL_PAIRS (assistant tool-call +
        tool observation) pairs, the oldest pairs are pruned from the middle of
        the history to prevent Groq 8k token limit errors on long agent runs.
        The most recent pairs (containing the most useful context) are retained.

    Phase 2 multi-turn format:
      - assistant turns with tool_call_id + tool_name are reconstructed with
        a tool_calls array (required by Groq to accept subsequent tool messages).
      - tool observation turns reference the tool_call_id from their assistant.
    """
    messages = [{"role": "system", "content": system_prompt}]

    if not history:
        # First iteration — seed the conversation with trigger/input data
        messages.append({
            "role": "user",
            "content": json.dumps(input_data, ensure_ascii=False),
        })
        return messages

    # Guard: ensure the history always starts with a user message
    if history[0].role != "user":
        messages.append({
            "role": "user",
            "content": json.dumps(input_data, ensure_ascii=False),
        })

    # --- Sliding-window pruning -------------------------------------------------
    # Identify contiguous (assistant tool-call, tool observation) pairs.
    # We keep the first user turn + any plain user/assistant turns, then
    # only the most recent _WINDOW_MAX_TOOL_PAIRS tool pairs.
    tool_pair_indices: List[Tuple[int, int]] = []  # (assistant_idx, tool_idx)
    i = 0
    while i < len(history) - 1:
        cur = history[i]
        nxt = history[i + 1]
        if (
            cur.role == "assistant" and cur.tool_call_id and cur.tool_name
            and nxt.role == "tool" and nxt.tool_call_id == cur.tool_call_id
        ):
            tool_pair_indices.append((i, i + 1))
            i += 2
        else:
            i += 1

    pruned_pair_indices: set = set()
    if len(tool_pair_indices) > _WINDOW_MAX_TOOL_PAIRS:
        pairs_to_drop = tool_pair_indices[: len(tool_pair_indices) - _WINDOW_MAX_TOOL_PAIRS]
        for a_idx, t_idx in pairs_to_drop:
            pruned_pair_indices.add(a_idx)
            pruned_pair_indices.add(t_idx)
        logger.info(
            "Sliding window: pruned %d oldest tool pairs from history (total pairs=%d)",
            len(pairs_to_drop),
            len(tool_pair_indices),
        )
    # ---------------------------------------------------------------------------

    for idx, turn in enumerate(history):
        if idx in pruned_pair_indices:
            continue  # skip pruned turns

        if turn.role == "assistant" and turn.tool_call_id and turn.tool_name:
            # Phase 2: reconstruct assistant message with tool_calls array
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
            messages.append({
                "role": "tool",
                "tool_call_id": turn.tool_call_id or "",
                "content": turn.content,
            })
        else:
            messages.append({"role": turn.role, "content": turn.content or ""})

    return messages


def _validate_arguments(
    arguments: dict,
    tool_id: str,
    tool_defs: List[ToolDefinition],
) -> List[str]:
    """
    Validate tool call arguments against the tool's JSON Schema parameters.
    Returns a list of human-readable error strings (empty = valid).

    Only checks 'required' fields — full JSON Schema validation would require
    jsonschema library. This lightweight check catches the most common
    hallucination: missing required parameters.
    """
    schema: Dict = {}
    for t in tool_defs:
        if t.tool_id == tool_id:
            schema = t.parameters or {}
            break

    required_fields: List[str] = schema.get("required", [])
    errors: List[str] = []
    for field in required_fields:
        if field not in arguments or arguments[field] is None or arguments[field] == "":
            errors.append(f"Required parameter '{field}' is missing or empty")
    return errors


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def _call_gemini_sync(
    api_key: str,
    model: str,
    system_prompt: str,
    history: List[ConversationTurn],
    input_data: dict
) -> Tuple[str, int]:
    import urllib.request
    import urllib.error
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    contents = []
    for turn in history:
        role = "user" if turn.role == "user" else "model"
        contents.append({"role": role, "parts": [{"text": turn.content or ""}]})
    if not contents and input_data:
        contents.append({"role": "user", "parts": [{"text": json.dumps(input_data)}]})

    body = {"contents": contents}
    if system_prompt:
        body["system_instruction"] = {"parts": [{"text": system_prompt}]}

    req_data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=req_data, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        resp_data = json.loads(resp.read().decode("utf-8"))
        candidates = resp_data.get("candidates", [])
        text = ""
        if candidates:
            parts = candidates[0].get("content", {}).get("parts", [])
            if parts:
                text = parts[0].get("text", "")
        tokens = resp_data.get("usageMetadata", {}).get("totalTokenCount", 0)
        return text, tokens


async def get_next_step(request: AgentNextStepRequest) -> AgentNextStepResponse:
    """
    Single ReAct turn: send conversation history + tool definitions to Gemini or Groq,
    parse the typed decision, return to Java.
    """
    if request.provider == "gemini":
        import asyncio
        import os
        api_key = request.api_key or os.getenv("GEMINI_API_KEY", "")
        model = request.model or os.getenv("GEMINI_MODEL", "gemini-3.5-flash-lite")
        if not api_key:
            return AgentNextStepResponse(
                decision="final_answer",
                final_answer="[Agent error: Gemini API key is missing]",
                tokens_used=0,
            )
        try:
            text, tokens = await asyncio.to_thread(
                _call_gemini_sync, api_key, model, request.system_prompt, request.conversation_history, request.input_data
            )
            return AgentNextStepResponse(
                decision="final_answer",
                final_answer=text,
                tokens_used=tokens,
            )
        except Exception as exc:
            logger.exception("Gemini call failed for session=%s iter=%d: %s", request.session_id, request.iteration, exc)
            return AgentNextStepResponse(
                decision="final_answer",
                final_answer=f"[Gemini API error: {exc}]",
                tokens_used=0,
            )

    if request.api_key:
        from groq import AsyncGroq
        client = AsyncGroq(api_key=request.api_key)
    else:
        client = get_groq_client()

    tools = [_to_groq_tool(t) for t in request.tool_definitions]
    messages = _build_messages(
        request.system_prompt,
        request.conversation_history,
        request.input_data,
    )

    # Use model from request if provided, otherwise fall back to default
    model = request.model or _DEFAULT_MODEL

    kwargs: dict = {
        "model": model,
        "messages": messages,
        "max_tokens": 1024,
        "temperature": 0.2,
    }

    # Only attach tools if the agent node has toolRefs configured.
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
        model=model,
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

        # ── Schema validation + internal retry ─────────────────────────────
        # Validate the LLM's arguments against the tool's required fields.
        # If required params are missing, do ONE internal retry with a correction
        # message before returning to Java.  This avoids the expensive round-trip
        # (Python → Java → tool handler → error observation → Python) for a
        # predictable class of hallucination errors.
        validation_errors = _validate_arguments(
            arguments, tc.function.name, request.tool_definitions
        )
        if validation_errors:
            logger.warning(
                "session=%s iter=%d — tool args validation failed for %s: %s — retrying internally",
                request.session_id,
                request.iteration,
                tc.function.name,
                validation_errors,
            )
            # Inject a correction turn and call Groq once more
            correction_messages = messages + [
                {
                    "role": "assistant",
                    "content": "",
                    "tool_calls": [{
                        "id": "retry_call",
                        "type": "function",
                        "function": {
                            "name": tc.function.name,
                            "arguments": tc.function.arguments or "{}",
                        },
                    }],
                },
                {
                    "role": "tool",
                    "tool_call_id": "retry_call",
                    "content": (
                        f"Error: your previous call to '{tc.function.name}' had invalid arguments. "
                        f"Issues: {'; '.join(validation_errors)}. "
                        "Please retry with all required parameters correctly filled in."
                    ),
                },
            ]
            try:
                retry_response = await client.chat.completions.create(
                    model=model,
                    messages=correction_messages,
                    tools=tools,
                    tool_choice="auto",
                    max_tokens=1024,
                    temperature=0.2,
                )
                retry_choice = retry_response.choices[0]
                retry_usage = retry_response.usage
                tokens_used += retry_usage.total_tokens if retry_usage else 0

                if retry_choice.finish_reason == "tool_calls" and retry_choice.message.tool_calls:
                    rtc = retry_choice.message.tool_calls[0]
                    try:
                        arguments = json.loads(rtc.function.arguments)
                        tc = rtc   # use the corrected tool call
                        logger.info(
                            "session=%s iter=%d — internal retry succeeded for %s",
                            request.session_id, request.iteration, tc.function.name,
                        )
                    except (json.JSONDecodeError, TypeError):
                        arguments = {}
            except Exception as retry_exc:
                logger.warning(
                    "session=%s iter=%d — internal retry call failed: %s",
                    request.session_id, request.iteration, retry_exc,
                )
        # ───────────────────────────────────────────────────────────────

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
    final = (choice.message.content or "").strip()
    if not final:
        final = "[Agent completed — no text response generated]"
        logger.warning(
            "session=%s iter=%d — LLM returned empty final_answer (finish_reason=%s)",
            request.session_id, request.iteration, choice.finish_reason,
        )
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
