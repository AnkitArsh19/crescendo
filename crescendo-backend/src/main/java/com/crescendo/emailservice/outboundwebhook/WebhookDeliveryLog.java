package com.crescendo.emailservice.outboundwebhook;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "webhook_delivery_logs",
    indexes = {
        @Index(name = "idx_webhook_delivery_sub", columnList = "subscriptionId"),
        @Index(name = "idx_webhook_delivery_status", columnList = "status"),
        @Index(name = "idx_webhook_delivery_retry", columnList = "nextRetryAt")
    })
public class WebhookDeliveryLog {

    public enum DeliveryStatus { PENDING, DELIVERED, FAILED }

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "subscriptionId", nullable = false)
    private UUID subscriptionId;

    @Column(name = "eventType", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "responseCode")
    private Integer responseCode;

    @Column(name = "latencyMs")
    private Long latencyMs;

    @Column(name = "attemptCount", nullable = false)
    private int attemptCount = 0;

    @Column(name = "nextRetryAt")
    private Instant nextRetryAt;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    public WebhookDeliveryLog() {}

    public WebhookDeliveryLog(UUID id, UUID subscriptionId, String eventType, Map<String, Object> payload, Instant nextRetryAt) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.payload = payload;
        this.nextRetryAt = nextRetryAt;
    }

    public UUID getId() { return id; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public String getEventType() { return eventType; }
    public Map<String, Object> getPayload() { return payload; }
    
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public Instant getCreatedAt() { return createdAt; }
}
