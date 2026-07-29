package com.crescendo.execution.agent;

import java.util.List;
import java.util.UUID;

/**
 * Configuration payload for an Agent Cluster Node.
 * Attached directly to a step's configuration payload under "agentConfig".
 */
public record AgentClusterConfig(
        List<UUID> toolRefs,
        UUID memoryRef,
        UUID modelRef,
        UUID outputParserRef,
        int maxIterations,
        int maxDelegationDepth,
        int tokenBudget
) {
    public AgentClusterConfig {
        if (toolRefs == null) toolRefs = List.of();
        if (maxIterations <= 0) maxIterations = 10;
        if (maxDelegationDepth <= 0) maxDelegationDepth = 3;
        if (tokenBudget <= 0) tokenBudget = 50000;
    }
}
