package com.crescendo.logbook.step_run;

import com.crescendo.enums.StepRunStatus;
import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import com.crescendo.logbook.domain_event.StepRunCompletedEvent;
import com.crescendo.security.DataSanitizationService;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Write-side service for step run management.
 *
 * Every mutation:
 *   1. Validates ownership of the parent workflow run
 *   2. Writes to the step_run table
 *   3. Publishes a domain event on completion
 */
@Service
@Transactional
public class StepRunService {

    private final StepRunRepository stepRunRepo;
    private final WorkflowRunRepository workflowRunRepo;
    private final DomainEventPublisher eventPublisher;
    private final DataSanitizationService sanitizationService;

    public StepRunService(StepRunRepository stepRunRepo,
                           WorkflowRunRepository workflowRunRepo,
                           DomainEventPublisher eventPublisher,
                           DataSanitizationService sanitizationService) {
        this.stepRunRepo = stepRunRepo;
        this.workflowRunRepo = workflowRunRepo;
        this.eventPublisher = eventPublisher;
        this.sanitizationService = sanitizationService;
    }

    /**
     * Creates a new step run record when the execution engine begins processing a step.
     */
    public LogbookDto.StepRunResponse startStepRun(UUID userId, UUID workflowRunId,
                                                    UUID stepId, Map<String, Object> inputData) {
        findOwnedRun(userId, workflowRunId);

        // Sanitize input before it is persisted — prevents PII/secrets leaking into logs.
        Map<String, Object> safeInput = sanitizationService.sanitize(inputData);

        UUID stepRunId = UUID.randomUUID();
        StepRun stepRun = new StepRun(stepRunId, workflowRunId, stepId,
                safeInput, StepRunStatus.RUNNING);
        stepRunRepo.save(stepRun);

        return toResponse(stepRun);
    }

    /**
     * Marks a step run as successfully completed with output data.
     */
    public void completeStepRun(UUID userId, UUID stepRunId, Map<String, Object> outputData) {
        StepRun stepRun = findOwnedStepRun(userId, stepRunId);

        if (stepRun.getStatus() != StepRunStatus.RUNNING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Step run must be RUNNING to complete, currently " + stepRun.getStatus());
        }

        // Sanitize output before it is persisted — prevents secrets from downstream steps leaking.
        stepRun.setStatus(StepRunStatus.SUCCESS);
        stepRun.setOutputData(sanitizationService.sanitize(outputData));
        stepRun.setCompletedAt(Instant.now());

        eventPublisher.publish(
                new StepRunCompletedEvent(stepRunId, stepRun.getWorkflowRunId(),
                        stepRun.getStepId(), StepRunStatus.SUCCESS, null));
    }

    /**
     * Marks a step run as failed with an error message.
     */
    public void failStepRun(UUID userId, UUID stepRunId, String errorMessage) {
        StepRun stepRun = findOwnedStepRun(userId, stepRunId);

        if (stepRun.getStatus() != StepRunStatus.RUNNING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Step run must be RUNNING to fail, currently " + stepRun.getStatus());
        }

        stepRun.setStatus(StepRunStatus.FAILED);
        stepRun.setErrorMessage(errorMessage);
        stepRun.setCompletedAt(Instant.now());

        eventPublisher.publish(
                new StepRunCompletedEvent(stepRunId, stepRun.getWorkflowRunId(),
                        stepRun.getStepId(), StepRunStatus.FAILED, errorMessage));
    }

    /**
     * Marks a step run as skipped (e.g. condition not met).
     */
    public void skipStepRun(UUID userId, UUID stepRunId) {
        StepRun stepRun = findOwnedStepRun(userId, stepRunId);

        if (stepRun.getStatus() != StepRunStatus.PENDING
                && stepRun.getStatus() != StepRunStatus.RUNNING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Step run is already in terminal state " + stepRun.getStatus());
        }

        stepRun.setStatus(StepRunStatus.SKIPPED);
        stepRun.setCompletedAt(Instant.now());

        eventPublisher.publish(
                new StepRunCompletedEvent(stepRunId, stepRun.getWorkflowRunId(),
                        stepRun.getStepId(), StepRunStatus.SKIPPED, null));
    }

    // -------------------------------------------------------------------------
    // OWNERSHIP VERIFICATION
    // -------------------------------------------------------------------------

    private WorkflowRun findOwnedRun(UUID userId, UUID workflowRunId) {
        return workflowRunRepo.findByIdAndUserId(workflowRunId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found"));
    }

    private StepRun findOwnedStepRun(UUID userId, UUID stepRunId) {
        StepRun stepRun = stepRunRepo.findById(stepRunId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Step run not found"));

        // Verify ownership through the parent workflow run
        workflowRunRepo.findByIdAndUserId(stepRun.getWorkflowRunId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this step run"));

        return stepRun;
    }

    // -------------------------------------------------------------------------
    // DTO MAPPER
    // -------------------------------------------------------------------------

    private LogbookDto.StepRunResponse toResponse(StepRun stepRun) {
        return new LogbookDto.StepRunResponse(
                stepRun.getId().toString(),
                stepRun.getStepId().toString(),
                stepRun.getStatus().name(),
                stepRun.getInputData(),
                stepRun.getOutputData(),
                stepRun.getErrorMessage(),
                stepRun.getCreatedAt(),
                stepRun.getCompletedAt()
        );
    }
}
