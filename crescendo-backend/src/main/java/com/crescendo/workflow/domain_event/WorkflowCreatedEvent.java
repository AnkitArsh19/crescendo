package com.crescendo.workflow.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a new workflow is created.
 */
public class WorkflowCreatedEvent extends BaseDomainEvent {

    private final String workflowName;
    private final UUID userId;
    private final String guestSessionId;

    public WorkflowCreatedEvent(UUID workflowId, String workflowName, UUID userId, String guestSessionId) {
        super(workflowId);
        this.workflowName = workflowName;
        this.userId = userId;
        this.guestSessionId = guestSessionId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getGuestSessionId() {
        return guestSessionId;
    }

    public boolean isGuestWorkflow() {
        return guestSessionId != null && userId == null;
    }
}
