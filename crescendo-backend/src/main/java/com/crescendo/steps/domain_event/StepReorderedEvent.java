package com.crescendo.steps.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a step's order is changed within its workflow.
 */
public class StepReorderedEvent extends BaseDomainEvent {

    private final UUID workflowId;

    public StepReorderedEvent(UUID stepId, UUID workflowId) {
        super(stepId);
        this.workflowId = workflowId;
    }

    public UUID getWorkflowId() { return workflowId; }
}
