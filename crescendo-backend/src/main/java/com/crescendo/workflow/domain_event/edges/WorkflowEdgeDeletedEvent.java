package com.crescendo.workflow.domain_event.edges;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when a workflow edge (connection between two steps) is deleted.
 */
public class WorkflowEdgeDeletedEvent extends BaseDomainEvent {

    private final UUID workflowId;

    public WorkflowEdgeDeletedEvent(UUID edgeId, UUID workflowId) {
        super(edgeId); // edgeId is the aggregate ID for this event
        this.workflowId = workflowId;
    }

    public UUID getWorkflowId() { return workflowId; }
}
