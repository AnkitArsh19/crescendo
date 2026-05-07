package com.crescendo.emailservice.broadcast;

import com.crescendo.enums.BroadcastStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A broadcast is a bulk email job that sends a template to all subscribed contacts.
 * Tracks fan-out progress: totalCount / sentCount / failedCount.
 */
@Entity
@Table(name = "email_broadcast",
        indexes = {
                @Index(name = "idx_broadcast_user", columnList = "userId")
        })
public class Broadcast {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID templateId;

    @Column(nullable = false, length = 320)
    private String fromAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BroadcastStatus status;

    @Column(nullable = false)
    private int totalCount;

    @Column(nullable = false)
    private int sentCount;

    @Column(nullable = false)
    private int failedCount;

    @Column(length = 2000)
    private String error;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant startedAt;

    @Column
    private Instant completedAt;

    protected Broadcast() {}

    public Broadcast(UUID id, UUID userId, UUID templateId, String fromAddress) {
        this.id = id;
        this.userId = userId;
        this.templateId = templateId;
        this.fromAddress = fromAddress;
        this.status = BroadcastStatus.DRAFT;
        this.totalCount = 0;
        this.sentCount = 0;
        this.failedCount = 0;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getTemplateId() { return templateId; }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public BroadcastStatus getStatus() { return status; }
    public void setStatus(BroadcastStatus status) { this.status = status; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getSentCount() { return sentCount; }
    public void setSentCount(int sentCount) { this.sentCount = sentCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
