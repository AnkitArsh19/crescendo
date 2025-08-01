package com.crescendo.logbook.StepRun;

import com.crescendo.enums.StepRunStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "step_run")
public class StepRun {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "workflow_run_id", nullable = false)
    private UUID workflowRunId;

    @Column(name = "step_run_id", nullable = false)
    private UUID stepRunId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> inputData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> outputData;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_run_status", nullable = false)
    private StepRunStatus status;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "completedAt", nullable = false)
    private Instant completedAt;

    public StepRun() {
    }

    public StepRun(UUID id, UUID workflowRunId, UUID stepRunId, Map<String, Object> inputData, Map<String, Object> outputData, StepRunStatus status, Instant createdAt, Instant completedAt) {
        this.id = id;
        this.workflowRunId = workflowRunId;
        this.stepRunId = stepRunId;
        this.inputData = inputData;
        this.outputData = outputData;
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

    public UUID getWorkflowRunId() {
        return workflowRunId;
    }

    public void setWorkflowRunId(UUID workflowRunId) {
        this.workflowRunId = workflowRunId;
    }

    public UUID getStepRunId() {
        return stepRunId;
    }

    public void setStepRunId(UUID stepRunId) {
        this.stepRunId = stepRunId;
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
