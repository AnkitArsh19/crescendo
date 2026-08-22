package com.crescendo.execution.agent;

import java.util.List;
import java.util.Map;

/**
 * Payload sent to the Python AI microservice on every ReAct loop iteration.
 *
 * <p>Java is the execution owner — it dispatches tools, holds credentials, and
 * persists checkpoints. Python is the reasoning owner — it receives the current
 * state and returns exactly one decision (tool call or final answer).</p>
 *
 * <p>The contract must stay stable: Python parses this with a Pydantic schema.
 * Always add fields as Optional on the Python side before using them here.</p>
 */
public record AgentNextStepRequest(

        /** Unique ID for this agent run (not the workflow run). Stable across turns. */
        String sessionId,

        /** 1-based iteration counter for the current turn. */
        int iteration,

        /** The system prompt configured on the canvas agent node. */
        String systemPrompt,

        /**
         * Tool definitions scoped ONLY to this agent's toolRefs —
         * never the full 114-app catalog. This prevents the agent from
         * calling apps the workflow owner never authorised.
         */
        List<ToolDefinition> toolDefinitions,

        /**
         * Full conversation history accumulated so far in this run.
         * Each entry has role (user|assistant|tool) and content.
         */
        List<ConversationTurn> conversationHistory,

        /**
         * Trigger payload or prior-step output — the data the agent is
         * reasoning about. Injected as the first "user" message if
         * conversationHistory is empty.
         */
        Map<String, Object> inputData,

        /**
         * AI model provider (gemini | openai | groq).
         */
        String provider,

        /**
         * Model identifier (e.g. gemini-2.5-flash, gpt-4o, llama-3.3-70b-versatile).
         */
        String model,

        /**
         * Effective API key for the provider (user BYOK or platform key).
         */
        String apiKey

) {

    // ── Nested schema types ───────────────────────────────────────────────

    /**
     * One tool the agent is allowed to call during this run.
     * Matches ToolDefinition in Python schemas/agent.py.
     */
    public record ToolDefinition(
            /** Matches toolRef step ID from AgentClusterConfig. */
            String toolId,
            String appKey,
            String actionKey,
            /** Human-readable description for LLM function-calling prompt. */
            String description,
            /** JSON Schema object describing the tool's required parameters. */
            Map<String, Object> parameters
    ) {}

    /**
     * One turn in the ReAct conversation.
     * role: "user" | "assistant" | "tool"
     */
    public record ConversationTurn(
            String role,
            String content,
            /** Present only when role == "tool". References the tool_call_id from the assistant turn. */
            String toolCallId
    ) {}
}
