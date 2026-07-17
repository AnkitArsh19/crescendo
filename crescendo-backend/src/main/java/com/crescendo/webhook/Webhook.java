package com.crescendo.webhook;

import com.crescendo.shared.domain.valueobject.WebhookKey;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook",
    indexes = {
        @Index(name = "idx_webhook_step", columnList = "stepId"),
        @Index(name = "idx_webhook_active", columnList = "isActive"),
        @Index(name = "idx_webhook_key", columnList = "webhookKey")
    })
public class Webhook {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "webhookKey", unique = true, nullable = false, length = 100))
    private WebhookKey webhookKey;

    @Column(name = "stepId", nullable = false)
    private UUID stepId;

    @Column(name = "isActive", nullable = false)
    private boolean isActive;

    // DEFAULT ensures ALTER TABLE ADD COLUMN NOT NULL succeeds on existing rows.
    // Existing webhooks get a random UUID-derived key; newly created ones still
    // use the constructor-assigned value.
    @Column(name = "secretKey", nullable = false, length = 128,
            columnDefinition = "varchar(128) default 'legacy-' || gen_random_uuid()")
    private String secretKey;

    // DEFAULT ensures ALTER TABLE ADD COLUMN NOT NULL succeeds on existing rows.
    @Column(name = "providerSignatureHeader", nullable = false, length = 64,
            columnDefinition = "varchar(64) default 'X-Hub-Signature-256'")
    private String providerSignatureHeader = "X-Hub-Signature-256";

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    public Webhook() {
    }

    public Webhook(UUID id, WebhookKey webhookKey, UUID stepId, boolean isActive) {
        this.id = id;
        this.webhookKey = webhookKey;
        this.stepId = stepId;
        this.isActive = isActive;
        this.secretKey = UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Convenience constructor accepting raw string for webhookKey.
     */
    public Webhook(UUID id, String webhookKey, UUID stepId, boolean isActive) {
        this(id, WebhookKey.of(webhookKey), stepId, isActive);
    }

    /**
     * Factory method to create a new webhook with auto-generated key.
     */
    public static Webhook create(UUID id, UUID stepId) {
        return new Webhook(id, WebhookKey.generate(), stepId, true);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public WebhookKey getWebhookKeyVO() {
        return webhookKey;
    }

    /**
     * Returns raw webhook key string for compatibility.
     */
    public String getWebhookKey() {
        return webhookKey != null ? webhookKey.value() : null;
    }

    public void setWebhookKey(WebhookKey webhookKey) {
        this.webhookKey = webhookKey;
    }

    public void setWebhookKey(String webhookKey) {
        this.webhookKey = WebhookKey.of(webhookKey);
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

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getProviderSignatureHeader() {
        return providerSignatureHeader;
    }

    public void setProviderSignatureHeader(String providerSignatureHeader) {
        this.providerSignatureHeader = providerSignatureHeader;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
