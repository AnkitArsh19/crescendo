package com.crescendo.logbook.WorkflowRun;

import com.crescendo.enums.WorkflowRunStatus;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
/// Indexes tell that when creating table create indexes for the given columns.
/// Here the selection process improves.
/// The index column is the name given, and it creates index from the column list given
@Table(name = "workflow_run",
    indexes = {
        @Index(name = "idx_workflow_run_workflow", columnList = "workflowId"),
        @Index(name = "idx_workflow_run_user", columnList = "userId"),
        @Index(name = "idx_workflow_run_status", columnList = "workflow_run_status")
    })
public class WorkflowRun {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "workflowId", nullable = false)
    private UUID workflowId;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> triggerData;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_run_status", nullable = false)
    private WorkflowRunStatus status;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "completedAt")
    private Instant completedAt;

    public WorkflowRun() {
    }

    public WorkflowRun(UUID id, UUID workflowId, UUID userId, Map<String, Object> triggerData, WorkflowRunStatus status, Instant createdAt, Instant completedAt) {
        this.id = id;
        this.workflowId = workflowId;
        this.userId = userId;
        this.triggerData = triggerData;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(UUID workflowId) {
        this.workflowId = workflowId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Map<String, Object> getTriggerData() {
        return triggerData;
    }

    public void setTriggerData(Map<String, Object> triggerData) {
        this.triggerData = triggerData;
    }

    public WorkflowRunStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowRunStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
