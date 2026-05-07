package com.crescendo.logbook.workflow_run;

import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.domain_event.WorkflowRunCompletedEvent;
import com.crescendo.logbook.domain_event.WorkflowRunStartedEvent;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.shared.infrastructure.event.RedisDomainEventPublisher;
import com.crescendo.workflow.workflow_command.Workflow_command;
import com.crescendo.workflow.workflow_command.Workflow_commandRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Write-side service for workflow run management.
 *
 * Every mutation:
 *   1. Validates ownership of the parent workflow
 *   2. Writes to the workflow_run table
 *   3. Publishes a domain event
 *   4. Enqueues execution to Redis for async processing
 */
@Service
@Transactional
public class WorkflowRunService {

    private final WorkflowRunRepository runRepo;
    private final Workflow_commandRepository workflowRepo;
    private final DomainEventPublisher eventPublisher;
    private final RedisDomainEventPublisher redisEventPublisher;

    public WorkflowRunService(WorkflowRunRepository runRepo,
                               Workflow_commandRepository workflowRepo,
                               DomainEventPublisher eventPublisher,
                               RedisDomainEventPublisher redisEventPublisher) {
        this.runRepo = runRepo;
        this.workflowRepo = workflowRepo;
        this.eventPublisher = eventPublisher;
        this.redisEventPublisher = redisEventPublisher;
    }

    /**
     * Starts a new workflow run for an authenticated user.
     * The parent workflow must exist, not be deleted, and be owned by the user.
     */
    public LogbookDto.WorkflowRunSummaryResponse startRun(UUID userId, UUID workflowId,
                                                           LogbookDto.StartWorkflowRunRequest req) {
        Workflow_command workflow = findOwnedWorkflow(userId, workflowId);

        if (!workflow.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Workflow must be active before it can be run");
        }

        UUID runId = UUID.randomUUID();
        WorkflowRun run = new WorkflowRun(runId, workflowId, userId,
                req.triggerData(), WorkflowRunStatus.PENDING);
        runRepo.save(run);

        eventPublisher.publish(
                new WorkflowRunStartedEvent(runId, workflowId, userId, req.triggerData()));

        // Enqueue for async execution by ExecutionQueueConsumer
        redisEventPublisher.enqueueExecution(Map.of(
                "workflowRunId", runId.toString(),
                "workflowId", workflowId.toString(),
                "userId", userId.toString()
        ));

        return toSummary(run, 0, 0, 0);
    }

    /**
     * Transitions a workflow run to RUNNING status.
     * Called by the execution engine when it picks up the run.
     */
    public void markRunning(UUID userId, UUID runId) {
        WorkflowRun run = findOwnedRun(userId, runId);

        if (run.getStatus() != WorkflowRunStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Run is already " + run.getStatus());
        }

        run.setStatus(WorkflowRunStatus.RUNNING);
    }

    /**
     * Marks a workflow run as successfully completed.
     * Called by the execution engine after all steps succeed.
     */
    public void completeRun(UUID userId, UUID runId) {
        WorkflowRun run = findOwnedRun(userId, runId);

        if (run.getStatus() != WorkflowRunStatus.RUNNING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Run must be RUNNING to complete, currently " + run.getStatus());
        }

        run.setStatus(WorkflowRunStatus.SUCCESS);
        run.setCompletedAt(Instant.now());

        eventPublisher.publish(
                new WorkflowRunCompletedEvent(runId, run.getWorkflowId(), userId,
                        WorkflowRunStatus.SUCCESS, null));
    }

    /**
     * Marks a workflow run as failed with an error message.
     * Called by the execution engine when a step fails.
     */
    public void failRun(UUID userId, UUID runId, String errorMessage) {
        WorkflowRun run = findOwnedRun(userId, runId);

        if (run.getStatus() != WorkflowRunStatus.RUNNING
                && run.getStatus() != WorkflowRunStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Run is already in terminal state " + run.getStatus());
        }

        run.setStatus(WorkflowRunStatus.FAILED);
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(Instant.now());

        eventPublisher.publish(
                new WorkflowRunCompletedEvent(runId, run.getWorkflowId(), userId,
                        WorkflowRunStatus.FAILED, errorMessage));
    }

    /**
     * Cancels a PENDING or RUNNING workflow run.
     */
    public void cancelRun(UUID userId, UUID workflowId, UUID runId) {
        findOwnedWorkflow(userId, workflowId);
        WorkflowRun run = findOwnedRun(userId, runId);

        if (run.getStatus() != WorkflowRunStatus.PENDING
                && run.getStatus() != WorkflowRunStatus.RUNNING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PENDING or RUNNING runs can be cancelled");
        }

        run.setStatus(WorkflowRunStatus.CANCELLED);
        run.setCompletedAt(Instant.now());

        eventPublisher.publish(
                new WorkflowRunCompletedEvent(runId, run.getWorkflowId(), userId,
                        WorkflowRunStatus.CANCELLED, null));
    }

    // -------------------------------------------------------------------------
    // OWNERSHIP VERIFICATION
    // -------------------------------------------------------------------------

    private Workflow_command findOwnedWorkflow(UUID userId, UUID workflowId) {
        Workflow_command workflow = workflowRepo.findByIdAndDeletedAtIsNull(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        if (workflow.getUser() == null || !workflow.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this workflow");
        }
        return workflow;
    }

    private WorkflowRun findOwnedRun(UUID userId, UUID runId) {
        return runRepo.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found"));
    }

    // -------------------------------------------------------------------------
    // DTO MAPPER
    // -------------------------------------------------------------------------

    private LogbookDto.WorkflowRunSummaryResponse toSummary(WorkflowRun run,
                                                             int totalSteps,
                                                             int completedSteps,
                                                             int failedSteps) {
        return new LogbookDto.WorkflowRunSummaryResponse(
                run.getId().toString(),
                run.getWorkflowId().toString(),
                run.getStatus().name(),
                run.getErrorMessage(),
                totalSteps,
                completedSteps,
                failedSteps,
                run.getCreatedAt(),
                run.getCompletedAt()
        );
    }
}
