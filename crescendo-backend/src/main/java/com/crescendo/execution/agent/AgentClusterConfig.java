package com.crescendo.execution.agent;

import java.util.List;
import java.util.UUID;

/**
 * Configuration payload for an Agent Cluster Node.
 * Attached directly to a step's configuration payload under "agentConfig".
 */
public record AgentClusterConfig(
        List<UUID> toolRefs,
        List<ToolConfig> configuredTools,
        UUID memoryRef,
        UUID modelRef,
        UUID outputParserRef,
        int maxIterations,
        int maxDelegationDepth,
        int tokenBudget
) {
    public AgentClusterConfig {
        if (toolRefs == null) toolRefs = List.of();
        if (configuredTools == null) configuredTools = List.of();
        if (maxIterations <= 0) maxIterations = 10;
        if (maxDelegationDepth <= 0) maxDelegationDepth = 3;
        if (tokenBudget <= 0) tokenBudget = 50000;
    }

    /** Overloaded constructor for backwards compatibility with tests and legacy callers. */
    public AgentClusterConfig(
            List<UUID> toolRefs,
            UUID memoryRef,
            UUID modelRef,
            UUID outputParserRef,
            int maxIterations,
            int maxDelegationDepth,
            int tokenBudget
    ) {
        this(toolRefs, List.of(), memoryRef, modelRef, outputParserRef, maxIterations, maxDelegationDepth, tokenBudget);
    }

    /**
     * Configuration for a single tool accessible to the agent.
     * Can represent either an app catalog action or a sub-workflow invocation.
     */
    public record ToolConfig(
            String toolId,
            String appKey,
            String actionKey,
            String customName,
            String customDescription,
            UUID connectionId,
            UUID subWorkflowId,
            java.util.Map<String, Object> fixedParameters
    ) {
        public ToolConfig(String appKey, String actionKey) {
            this(null, appKey, actionKey, null, null, null, null, java.util.Map.of());
        }
    }
}
