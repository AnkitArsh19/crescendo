package com.crescendo.workflow.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a workflow is soft-deleted.
 */
public class WorkflowDeletedEvent extends BaseDomainEvent {

    public WorkflowDeletedEvent(UUID workflowId) {
        super(workflowId);
    }
}
