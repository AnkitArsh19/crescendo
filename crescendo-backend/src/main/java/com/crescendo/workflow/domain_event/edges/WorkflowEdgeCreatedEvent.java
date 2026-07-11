package com.crescendo.workflow.domain_event.edges;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when a new workflow edge (connection between two steps) is created.
 */
public class WorkflowEdgeCreatedEvent extends BaseDomainEvent {

    private final UUID workflowId;
    private final UUID sourceStepId;
    private final UUID targetStepId;
    private final String sourceHandle;
    private final String targetHandle;

    public WorkflowEdgeCreatedEvent(UUID edgeId, UUID workflowId,
                                     UUID sourceStepId, UUID targetStepId,
                                     String sourceHandle, String targetHandle) {
        super(edgeId); // edgeId is the aggregate ID for this event
        this.workflowId = workflowId;
        this.sourceStepId = sourceStepId;
        this.targetStepId = targetStepId;
        this.sourceHandle = sourceHandle;
        this.targetHandle = targetHandle;
    }

    public UUID getWorkflowId() { return workflowId; }
    public UUID getSourceStepId() { return sourceStepId; }
    public UUID getTargetStepId() { return targetStepId; }
    public String getSourceHandle() { return sourceHandle; }
    public String getTargetHandle() { return targetHandle; }
}
