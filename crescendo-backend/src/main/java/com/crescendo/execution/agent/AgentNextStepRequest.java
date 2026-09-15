package com.crescendo.execution.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Payload sent to the Python AI microservice on every ReAct loop iteration.
 *
 * <p>Java is the execution owner: it dispatches tools, holds credentials, and
 * persists checkpoints. Python is the reasoning owner: it receives the current
 * state and returns exactly one decision (tool call or final answer).</p>
 *
 * <p>The contract must stay stable: Python parses this with a Pydantic schema.
 * Always add fields as Optional on the Python side before using them here.</p>
 */
public record AgentNextStepRequest(

        /** Unique ID for this agent run (not the workflow run). Stable across turns. */
        @JsonProperty("session_id")
        String sessionId,

        /** 1-based iteration counter for the current turn. */
        @JsonProperty("iteration")
        int iteration,

        /** The system prompt configured on the canvas agent node. */
        @JsonProperty("system_prompt")
        String systemPrompt,

        /**
         * Tool definitions scoped ONLY to this agent's toolRefs:
         * never the full 114-app catalog. This prevents the agent from
         * calling apps the workflow owner never authorised.
         */
        @JsonProperty("tool_definitions")
        List<ToolDefinition> toolDefinitions,

        /**
         * Full conversation history accumulated so far in this run.
         * Each entry has role (user|assistant|tool) and content.
         */
        @JsonProperty("conversation_history")
        List<ConversationTurn> conversationHistory,

        /**
         * Trigger payload or prior-step output: the data the agent is
         * reasoning about. Injected as the first "user" message if
         * conversationHistory is empty.
         */
        @JsonProperty("input_data")
        Map<String, Object> inputData,

        /**
         * AI model provider (gemini | openai | groq).
         */
        @JsonProperty("provider")
        String provider,

        /**
         * Model identifier (e.g. gemini-3.8-flash, gpt-4o, llama-3.3-70b-versatile).
         */
        @JsonProperty("model")
        String model,

        /**
         * Effective API key for the provider (user BYOK or platform key).
         */
        @JsonProperty("api_key")
        String apiKey

) {

    // Nested schema types

    /**
     * One tool the agent is allowed to call during this run.
     * Matches ToolDefinition in Python schemas/agent.py.
     */
    public record ToolDefinition(
            /** Matches toolRef step ID from AgentClusterConfig. */
            @JsonProperty("tool_id")
            String toolId,

            @JsonProperty("app_key")
            String appKey,

            @JsonProperty("action_key")
            String actionKey,

            /** Human-readable description for LLM function-calling prompt. */
            @JsonProperty("description")
            String description,

            /** JSON Schema object describing the tool's required parameters. */
            @JsonProperty("parameters")
            Map<String, Object> parameters
    ) {}

    /**
     * One turn in the ReAct conversation.
     * role: "user" | "assistant" | "tool"
     */
    public record ConversationTurn(
            @JsonProperty("role")
            String role,

            @JsonProperty("content")
            String content,

            /** Present only when role == "tool". References the tool_call_id from the assistant turn. */
            @JsonProperty("tool_call_id")
            String toolCallId,

            /** Phase 2: function name in the tool_calls array (assistant turns). */
            @JsonProperty("tool_name")
            String toolName,

            /** Phase 2: JSON string of arguments (assistant turns). */
            @JsonProperty("tool_args_json")
            String toolArgsJson
    ) {
        public ConversationTurn(String role, String content, String toolCallId) {
            this(role, content, toolCallId, null, null);
        }
    }
}
