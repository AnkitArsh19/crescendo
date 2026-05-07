package com.crescendo.workflow.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a workflow is activated.
 */
public class WorkflowActivatedEvent extends BaseDomainEvent {

    public WorkflowActivatedEvent(UUID workflowId) {
        super(workflowId);
    }
}
