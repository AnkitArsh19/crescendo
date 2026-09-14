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
    private final com.crescendo.execution.engine.WorkflowExecutionEngine executionEngine;
    private final com.crescendo.logbook.workflow_run.WorkflowRunRepository runRepo;
    private final com.crescendo.workflow.workflow_command.Workflow_commandRepository workflowRepo;

    public SubWorkflowToolRunner(
            DistributedLockService lockService,
            @org.springframework.context.annotation.Lazy com.crescendo.execution.engine.WorkflowExecutionEngine executionEngine,
            com.crescendo.logbook.workflow_run.WorkflowRunRepository runRepo,
            com.crescendo.workflow.workflow_command.Workflow_commandRepository workflowRepo
    ) {
        this.lockService = lockService;
        this.executionEngine = executionEngine;
        this.runRepo = runRepo;
        this.workflowRepo = workflowRepo;
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

            var workflowOpt = workflowRepo.findById(subWorkflowId);
            if (workflowOpt.isEmpty()) {
                log.warn("Sub-workflow {} not found for tool execution", subWorkflowId);
                return Map.of("status", "NOT_FOUND", "error", "Sub-workflow " + subWorkflowId + " not found");
            }

            var workflow = workflowOpt.get();
            if (workflow.getUser() != null && !workflow.getUser().getId().equals(ownerUserId)) {
                log.warn("Access denied for user {} on sub-workflow {}", ownerUserId, subWorkflowId);
                return Map.of("status", "FORBIDDEN", "error", "Access denied for sub-workflow " + subWorkflowId);
            }

            UUID runId = UUID.randomUUID();
            com.crescendo.logbook.workflow_run.WorkflowRun run = new com.crescendo.logbook.workflow_run.WorkflowRun(
                    runId,
                    subWorkflowId,
                    ownerUserId,
                    inputParams != null ? new java.util.HashMap<>(inputParams) : new java.util.HashMap<>(),
                    com.crescendo.enums.WorkflowRunStatus.RUNNING
            );
            runRepo.save(run);

            // Synchronously execute the sub-workflow in-process on virtual thread
            executionEngine.execute(run);

            // Fetch final status
            var refreshed = runRepo.findById(runId).orElse(run);
            boolean success = refreshed.getStatus() == com.crescendo.enums.WorkflowRunStatus.SUCCESS;

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("status", success ? "SUCCESS" : "FAILURE");
            result.put("subWorkflowRunId", runId.toString());
            result.put("subWorkflowId", subWorkflowId.toString());
            if (refreshed.getExecutionState() != null && !refreshed.getExecutionState().isEmpty()) {
                result.put("output", refreshed.getExecutionState());
                result.put("data", refreshed.getExecutionState());
            } else if (!success) {
                result.put("error", refreshed.getErrorMessage() != null ? refreshed.getErrorMessage() : "Sub-workflow execution failed");
            } else {
                result.put("output", Map.of("message", "Sub-workflow executed successfully"));
            }

            return result;
        } catch (Exception e) {
            log.error("Sub-workflow tool execution failed for {}: {}", subWorkflowId, e.getMessage(), e);
            return Map.of(
                    "status", "ERROR",
                    "error", "Failed to execute sub-workflow: " + e.getMessage()
            );
        } finally {
            lockService.unlock(lockKey, lockToken.get());
            log.info("Released lock for sub-workflow tool: key={}", lockKey);
        }
    }
}
