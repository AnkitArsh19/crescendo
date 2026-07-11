package com.crescendo.workflow.workflow_query;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "workflow_edge_query",
    indexes = {
        @Index(name = "idx_edge_query_workflow", columnList = "workflowId"),
        @Index(name = "idx_edge_query_source", columnList = "sourceStepId"),
        @Index(name = "idx_edge_query_target", columnList = "targetStepId")
    })
public class WorkflowEdge_query {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "workflowId", nullable = false)
    private UUID workflowId;

    @Column(name = "sourceStepId", nullable = false)
    private UUID sourceStepId;

    @Column(name = "targetStepId", nullable = false)
    private UUID targetStepId;

    @Column(name = "sourceHandle", length = 100)
    private String sourceHandle;

    @Column(name = "targetHandle", length = 100)
    private String targetHandle;

    public WorkflowEdge_query() {
    }

    public WorkflowEdge_query(UUID id, UUID workflowId, UUID sourceStepId, UUID targetStepId, String sourceHandle, String targetHandle) {
        this.id = id;
        this.workflowId = workflowId;
        this.sourceStepId = sourceStepId;
        this.targetStepId = targetStepId;
        this.sourceHandle = sourceHandle;
        this.targetHandle = targetHandle;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public UUID getSourceStepId() {
        return sourceStepId;
    }

    public UUID getTargetStepId() {
        return targetStepId;
    }

    public String getSourceHandle() {
        return sourceHandle;
    }

    public String getTargetHandle() {
        return targetHandle;
    }
}
