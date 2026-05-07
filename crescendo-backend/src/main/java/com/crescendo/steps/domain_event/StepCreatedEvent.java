package com.crescendo.steps.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a new step is added to a workflow.
 */
public class StepCreatedEvent extends BaseDomainEvent {

    private final UUID workflowId;
    private final String stepName;

    public StepCreatedEvent(UUID stepId, UUID workflowId, String stepName) {
        super(stepId);
        this.workflowId = workflowId;
        this.stepName = stepName;
    }

    public UUID getWorkflowId() { return workflowId; }
    public String getStepName() { return stepName; }
}
