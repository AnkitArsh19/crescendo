package com.crescendo.execution.agent;

import com.crescendo.shared.infrastructure.lock.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Executes a child sub-workflow as an atomic callable tool for an Agent.
 * Fulfills the "assign an agent to a specific task" pattern.
 *
 * Mandatory Concurrency Discipline:
 * Acquires DistributedLockService on "workflow-execution:{subWorkflowId}" before execution
 * to prevent double-execution race conditions with independent triggers.
 */
@Component
public class SubWorkflowToolRunner {

    private static final Logger log = LoggerFactory.getLogger(SubWorkflowToolRunner.class);
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    private final DistributedLockService lockService;

    public SubWorkflowToolRunner(DistributedLockService lockService) {
        this.lockService = lockService;
    }

    public Map<String, Object> executeSubWorkflowTool(
            UUID subWorkflowId,
            UUID ownerUserId,
            Map<String, Object> inputParams
    ) {
        String lockKey = "workflow-execution:" + subWorkflowId;

        log.info("Attempting lock acquisition for sub-workflow tool: key={} owner={}", lockKey, ownerUserId);

        java.util.Optional<String> lockToken = lockService.tryLock(lockKey, LOCK_TTL.toMillis());
        if (lockToken.isEmpty()) {
            log.warn("Failed to acquire lock for sub-workflow {}; concurrent execution in progress.", subWorkflowId);
            return Map.of(
                    "status", "LOCKED",
                    "error", "Sub-workflow is currently executing under another lock key=" + lockKey
            );
        }

        try {
            log.info("Acquired lock {}. Executing sub-workflow {} as tool with input params={}",
                    lockKey, subWorkflowId, inputParams);

            // TODO (Phase 2 — Sub-workflow tool wiring):
            // Inject WorkflowExecutionEngine and call executeSubWorkflow(subWorkflowId, ownerUserId, inputParams).
            // This requires WorkflowExecutionEngine to expose a synchronous executeSubWorkflow() method that
            // returns the final step output without creating a full async WorkflowRun record, OR
            // creates a WorkflowRun with RUN_AS_TOOL status and blocks until it completes.
            //
            // The lock discipline here (tryLock → execute → unlock) is correct and must be preserved.
            // Only the inner execution call needs to be wired.
            //
            // Until this is implemented, agent nodes that invoke sub-workflows will receive an error
            // observation, which the LLM can handle gracefully by attempting a different tool.
            throw new UnsupportedOperationException(
                    "Sub-workflow tool execution is not yet implemented. " +
                    "Wire WorkflowExecutionEngine.executeSubWorkflow() here. subWorkflowId=" + subWorkflowId
            );
        } finally {
            lockService.unlock(lockKey, lockToken.get());
            log.info("Released lock for sub-workflow tool: key={}", lockKey);
        }
    }
}
