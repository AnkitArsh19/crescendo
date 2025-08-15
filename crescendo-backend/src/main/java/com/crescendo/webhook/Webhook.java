package com.crescendo.webhook;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
/// Indexes tell that when creating table create indexes for the given columns.
/// Here the selection process improves.
/// The index column is the name given, and it creates index from the column list given
@Table(name = "webhook",
    indexes = {
        @Index(name = "idx_webhook_step", columnList = "stepId"),
        @Index(name = "idx_webhook_active", columnList = "isActive")
    })
public class Webhook {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "webhookKey", unique = true, nullable = false)
    private String webhookKey;

    @Column(name = "stepId", nullable = false)
    private UUID stepId;

    @Column(name = "isActive", nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    public Webhook() {
    }

    public Webhook(UUID id, String webhookKey, UUID stepId, boolean isActive, Instant createdAt) {
        this.id = id;
        this.webhookKey = webhookKey;
        this.stepId = stepId;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getWebhookKey() {
        return webhookKey;
    }

    public void setWebhookKey(String webhookKey) {
        this.webhookKey = webhookKey;
    }

    public UUID getStepId() {
        return stepId;
    }

    public void setStepId(UUID stepId) {
        this.stepId = stepId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
