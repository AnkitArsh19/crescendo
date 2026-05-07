package com.crescendo.steps.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a step is soft-deleted from a workflow.
 */
public class StepDeletedEvent extends BaseDomainEvent {

    private final UUID workflowId;

    public StepDeletedEvent(UUID stepId, UUID workflowId) {
        super(stepId);
        this.workflowId = workflowId;
    }

    public UUID getWorkflowId() { return workflowId; }
}
