package com.crescendo.execution.suspension;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_suspension",
    indexes = {
        @Index(name = "idx_suspension_run_id", columnList = "runId"),
        @Index(name = "idx_suspension_correlation", columnList = "correlationKey", unique = true),
        @Index(name = "idx_suspension_status", columnList = "status"),
        @Index(name = "idx_suspension_timeout", columnList = "timeoutAt")
    })
public class WorkflowSuspension {

    public enum SuspensionStatus {
        WAITING,
        RESUMED,
        TIMED_OUT
    }

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "runId", nullable = false)
    private UUID runId;

    @Column(name = "stepId", nullable = false)
    private UUID stepId;

    /** E.g. "email:1234:delivered" or "approval:abcd" */
    @Column(name = "correlationKey", nullable = false, length = 255)
    private String correlationKey;

    /** The resumeToken passed to WorkflowRunService when suspending */
    @Column(name = "resumeToken", nullable = false, length = 255)
    private String resumeToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SuspensionStatus status = SuspensionStatus.WAITING;

    @Column(name = "timeoutAt")
    private Instant timeoutAt;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "resumedAt")
    private Instant resumedAt;

    public WorkflowSuspension() {}

    public WorkflowSuspension(UUID id, UUID runId, UUID stepId, String correlationKey, String resumeToken, Instant timeoutAt) {
        this.id = id;
        this.runId = runId;
        this.stepId = stepId;
        this.correlationKey = correlationKey;
        this.resumeToken = resumeToken;
        this.timeoutAt = timeoutAt;
        this.status = SuspensionStatus.WAITING;
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public UUID getStepId() { return stepId; }
    public String getCorrelationKey() { return correlationKey; }
    public String getResumeToken() { return resumeToken; }
    public SuspensionStatus getStatus() { return status; }
    public void setStatus(SuspensionStatus status) { this.status = status; }
    public Instant getTimeoutAt() { return timeoutAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResumedAt() { return resumedAt; }
    public void setResumedAt(Instant resumedAt) { this.resumedAt = resumedAt; }
}
