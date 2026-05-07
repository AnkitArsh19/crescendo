package com.crescendo.steps.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a step's configuration is updated.
 */
public class StepUpdatedEvent extends BaseDomainEvent {

    private final UUID workflowId;

    public StepUpdatedEvent(UUID stepId, UUID workflowId) {
        super(stepId);
        this.workflowId = workflowId;
    }

    public UUID getWorkflowId() { return workflowId; }
}
