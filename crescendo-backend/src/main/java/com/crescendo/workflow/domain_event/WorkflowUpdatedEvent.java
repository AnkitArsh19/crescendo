package com.crescendo.workflow.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a workflow's metadata (name, description) is updated.
 */
public class WorkflowUpdatedEvent extends BaseDomainEvent {

    public WorkflowUpdatedEvent(UUID workflowId) {
        super(workflowId);
    }
}
