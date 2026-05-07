package com.crescendo.workflow.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a workflow is deactivated.
 */
public class WorkflowDeactivatedEvent extends BaseDomainEvent {

    public WorkflowDeactivatedEvent(UUID workflowId) {
        super(workflowId);
    }
}
