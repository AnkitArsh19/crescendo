package com.crescendo.logbook.domain_event;

import com.crescendo.enums.StepRunStatus;
import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a step run reaches a terminal state (SUCCESS, FAILED, or SKIPPED).
 */
public class StepRunCompletedEvent extends BaseDomainEvent {

    private final UUID workflowRunId;
    private final UUID stepId;
    private final StepRunStatus status;
    private final String errorMessage;

    public StepRunCompletedEvent(UUID stepRunId, UUID workflowRunId,
                                  UUID stepId, StepRunStatus status, String errorMessage) {
        super(stepRunId);
        this.workflowRunId = workflowRunId;
        this.stepId = stepId;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public UUID getWorkflowRunId() {
        return workflowRunId;
    }

    public UUID getStepId() {
        return stepId;
    }

    public StepRunStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccessful() {
        return status == StepRunStatus.SUCCESS;
    }
}
