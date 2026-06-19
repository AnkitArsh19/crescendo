package com.crescendo.logbook.workflow_run;

import com.crescendo.enums.WorkflowRunStatus;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
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
        @Index(name = "idx_workflow_run_status", columnList = "workflow_run_status"),
        @Index(name = "idx_workflow_run_resume_token", columnList = "resume_token")
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
    @Column(name = "workflow_run_status", nullable = false, length = 20)
    private WorkflowRunStatus status;

    /**
     * Error message if workflow failed, null otherwise.
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "completedAt")
    private Instant completedAt;

    @Column(name = "resume_at")
    private Instant resumeAt;

    @Column(name = "resume_step_id")
    private UUID resumeStepId;

    @Column(name = "resume_token", length = 120)
    private String resumeToken;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_state", columnDefinition = "jsonb")
    private Map<String, Object> executionState;

    public WorkflowRun() {
    }

    public WorkflowRun(UUID id, UUID workflowId, UUID userId, Map<String, Object> triggerData, WorkflowRunStatus status) {
        this.id = id;
        this.workflowId = workflowId;
        this.userId = userId;
        this.triggerData = triggerData;
        this.status = status;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public Instant getResumeAt() {
        return resumeAt;
    }

    public void setResumeAt(Instant resumeAt) {
        this.resumeAt = resumeAt;
    }

    public UUID getResumeStepId() {
        return resumeStepId;
    }

    public void setResumeStepId(UUID resumeStepId) {
        this.resumeStepId = resumeStepId;
    }

    public String getResumeToken() {
        return resumeToken;
    }

    public void setResumeToken(String resumeToken) {
        this.resumeToken = resumeToken;
    }

    public Map<String, Object> getExecutionState() {
        return executionState;
    }

    public void setExecutionState(Map<String, Object> executionState) {
        this.executionState = executionState;
    }
}
