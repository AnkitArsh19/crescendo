package com.crescendo.logbook.step_run;

import com.crescendo.enums.StepRunStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
/// Indexes tell that when creating table create indexes for the given columns.
/// Here the selection process improves.
/// The index column is the name given, and it creates index from the column list given
@Table(name = "step_run",
    indexes = {
        @Index(name = "idx_step_run_workflow_run", columnList = "workflow_run_id"),
        @Index(name = "idx_step_run_step", columnList = "step_id"),
        @Index(name = "idx_step_run_status", columnList = "step_run_status")
    })
public class StepRun {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "workflow_run_id", nullable = false)
    private UUID workflowRunId;

    @Column(name = "step_id", nullable = false)
    private UUID stepId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> inputData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_data", columnDefinition = "jsonb")
    private Map<String, Object> outputData;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_run_status", nullable = false, length = 20)
    private StepRunStatus status;

    /**
     * Error message if step failed, null otherwise.
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "completedAt")
    private Instant completedAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private Instant updatedAt;

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public StepRun() {
    }

    public StepRun(UUID id, UUID workflowRunId, UUID stepId, Map<String, Object> inputData, StepRunStatus status) {
        this.id = id;
        this.workflowRunId = workflowRunId;
        this.stepId = stepId;
        this.inputData = inputData;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkflowRunId() {
        return workflowRunId;
    }

    public void setWorkflowRunId(UUID workflowRunId) {
        this.workflowRunId = workflowRunId;
    }

    public UUID getStepId() {
        return stepId;
    }

    public void setStepId(UUID stepId) {
        this.stepId = stepId;
    }

    public Map<String, Object> getInputData() {
        return inputData;
    }

    public void setInputData(Map<String, Object> inputData) {
        this.inputData = inputData;
    }

    public Map<String, Object> getOutputData() {
        return outputData;
    }

    public void setOutputData(Map<String, Object> outputData) {
        this.outputData = outputData;
    }

    public StepRunStatus getStatus() {
        return status;
    }

    public void setStatus(StepRunStatus status) {
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
}
