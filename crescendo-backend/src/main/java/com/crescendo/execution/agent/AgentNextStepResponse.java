package com.crescendo.execution.agent;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload returned by the Python AI microservice for each ReAct turn.
 *
 * <p>Java receives this, checks the decision, and either:</p>
 * <ul>
 *   <li>{@code tool_call} — dispatches the named tool through {@link ActionHandlerRegistry},
 *       sanitises the output, appends it to conversation history, then loops.</li>
 *   <li>{@code final_answer} — exits the loop and returns the answer as the step output.</li>
 * </ul>
 *
 * <p>Python writes this via the {@code AgentNextStepResponse} Pydantic model in
 * {@code crescendo-aiml/app/schemas/agent.py}. Field names must match exactly.</p>
 */
public record AgentNextStepResponse(

        /**
         * "tool_call" — agent wants to invoke a tool.
         * "final_answer" — agent has finished reasoning.
         */
        String decision,

        /**
         * Populated when decision == "tool_call".
         * Null otherwise — always null-check before use.
         */
        ToolCallDecision toolCall,

        /**
         * Populated when decision == "final_answer".
         * The agent's final output string.
         */
        String finalAnswer,

        /**
         * Chain-of-thought reasoning text from the LLM (optional).
         * Logged for observability; never executed or forwarded to tools.
         */
        String reasoning,

        /**
         * Token count for this single turn, reported by the LLM API.
         * Accumulated in AgentExecutionService against tokenBudget.
         */
        @JsonProperty("tokens_used") int tokensUsed

) {

    // ── Nested type ───────────────────────────────────────────────────────

    /**
     * The tool the agent decided to call.
     * Matches ToolCall in Python schemas/agent.py.
     */
    public record ToolCallDecision(
            /** Matches AgentNextStepRequest.ToolDefinition.toolId (the step UUID string). */
            String toolId,
            String appKey,
            String actionKey,
            /** Key-value map of resolved parameter values for the chosen action. */
            java.util.Map<String, Object> arguments
    ) {}

    // ── Convenience helpers ───────────────────────────────────────────────

    public boolean isToolCall() {
        return "tool_call".equals(decision) && toolCall != null;
    }

    public boolean isFinalAnswer() {
        return "final_answer".equals(decision);
    }
}
