"""
Agent Executor Schemas
=======================
Defines the exact JSON contract between Java's AgentExecutionService and
the Python /v1/agent/next-step endpoint.

Both Phase 1 and Phase 2 fields are included here:
  - Phase 1: core request/response contract
  - Phase 2: ConversationTurn.tool_name / tool_args_json for Groq-compatible
             multi-turn message format (required for agents that call >1 tool)

IMPORTANT: Java must populate tool_name and tool_args_json on assistant turns
that contain a tool call decision, otherwise multi-turn runs will fail on the
second Groq call with a 400 error.
"""

from typing import Any, Dict, List, Literal, Optional

from pydantic import BaseModel, Field


class ToolDefinition(BaseModel):
    """
    One tool the agent is allowed to call during this run.

    Java builds this list from AgentClusterConfig.toolRefs() only —
    never the full 114-app catalog.  Scoping to toolRefs enforces the
    permission boundary defined by the workflow owner.
    """

    tool_id: str          # matches toolRef step ID from AgentClusterConfig
    app_key: str          # e.g. "slack"
    action_key: str       # e.g. "slack:post_message"
    description: str      # human-readable description for the LLM prompt
    parameters: Dict[str, Any] = Field(default_factory=dict)  # JSON Schema of config fields


class ConversationTurn(BaseModel):
    """
    One turn in the conversation history sent by Java.

    Phase 2 additions (tool_name, tool_args_json):
      Required when role == "assistant" and the turn contained a tool call.
      These are used to reconstruct the Groq-required tool_calls array format
      so that subsequent "tool" role messages can reference the correct
      tool_call_id.  Without this, Groq rejects the message sequence on
      iteration 2+ of any multi-tool run.

    These fields are Optional to maintain backward compatibility with
    Phase 1 conversation histories that have tool_call_id but not tool_name.
    """

    role: Literal["user", "assistant", "tool"]
    content: str = ""   # default="" — Groq returns None content on pure tool-call assistant turns
    tool_call_id: Optional[str] = None    # present when role == "tool" or assistant with tool call

    # Phase 2 additions — required for Groq tool_calls message format
    tool_name: Optional[str] = None       # function name in the tool_calls array (assistant turns)
    tool_args_json: Optional[str] = None  # JSON string of arguments (assistant turns)


class AgentNextStepRequest(BaseModel):
    """
    Sent by Java AgentExecutionService on every ReAct loop iteration.

    Java is the state owner — it sends the full conversation history each
    time rather than maintaining a server-side session.  Python remains
    completely stateless across turns.  This trades request size for
    operational simplicity, which is the correct trade-off here.
    """

    session_id: str                          # unique per agent run (not per workflow run)
    iteration: int                           # 1-based turn counter
    system_prompt: str                       # from step configuration.systemPrompt
    tool_definitions: List[ToolDefinition]   # scoped to toolRefs only
    conversation_history: List[ConversationTurn]
    input_data: Dict[str, Any]              # trigger payload / prior step output
    model: Optional[str] = None             # override LLM model; falls back to default if omitted


class ToolCall(BaseModel):
    """The resolved tool call returned to Java when decision == 'tool_call'."""

    tool_id: str               # matches ToolDefinition.tool_id
    app_key: str
    action_key: str
    arguments: Dict[str, Any]  # resolved parameter values from LLM


class AgentNextStepResponse(BaseModel):
    """
    Returned to Java.  Java dispatches the tool and observes the result.

    Exactly one of tool_call or final_answer will be populated, determined
    by the 'decision' field.  reasoning is chain-of-thought logged for
    observability but never executed.
    """

    decision: Literal["tool_call", "final_answer"]
    tool_call: Optional[ToolCall] = None       # present when decision == "tool_call"
    final_answer: Optional[str] = None         # present when decision == "final_answer"
    reasoning: Optional[str] = None            # chain-of-thought (logged, not executed)
    tokens_used: int = 0
