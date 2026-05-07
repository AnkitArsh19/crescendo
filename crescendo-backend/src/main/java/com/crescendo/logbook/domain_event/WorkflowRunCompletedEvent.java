package com.crescendo.logbook.domain_event;

import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a workflow run is completed (success or failure).
 */
public class WorkflowRunCompletedEvent extends BaseDomainEvent {

    private final UUID workflowId;
    private final UUID userId;
    private final WorkflowRunStatus status;
    private final String errorMessage;

    public WorkflowRunCompletedEvent(UUID workflowRunId, UUID workflowId, UUID userId, 
                                      WorkflowRunStatus status, String errorMessage) {
        super(workflowRunId);
        this.workflowId = workflowId;
        this.userId = userId;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public UUID getUserId() {
        return userId;
    }

    public WorkflowRunStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccessful() {
        return status == WorkflowRunStatus.SUCCESS;
    }
}
