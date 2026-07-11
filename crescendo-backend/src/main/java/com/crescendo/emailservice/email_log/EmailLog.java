package com.crescendo.emailservice.email_log;

import com.crescendo.enums.EmailStatus;
import com.crescendo.enums.EmailType;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Definitive record of every email processed through the system.
 * Tracks the full lifecycle: PENDING → SENT → DELIVERED (or FAILED/BOUNCED).
 * Shared across command and query databases for write + read access.
 */
@Entity
@Table(name = "email_log",
    indexes = {
        @Index(name = "idx_email_log_user",   columnList = "userId"),
        @Index(name = "idx_email_log_status", columnList = "status"),
        @Index(name = "idx_email_log_sent_at",columnList = "sentAt"),
        @Index(name = "idx_email_log_apikey", columnList = "appKeyId")
        // GIN index for tags is created via native DDL:
        // CREATE INDEX idx_email_log_tags ON email_log USING GIN (tags jsonb_path_ops);
    })
public class EmailLog {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "appKeyId", nullable = false)
    private UUID appKeyId;

    @Column(name = "toAddress", nullable = false, length = 320)
    private String toAddress;

    @Column(name = "fromAddress", nullable = false, length = 320)
    private String fromAddress;

    @Column(name = "subject", nullable = false, length = 1000)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 20)
    private EmailType emailType;

    @Column(name = "providerMessageId", length = 255)
    private String providerMessageId;

    @Column(name = "error", length = 2000)
    private String error;

    @Column(name = "templateId")
    private UUID templateId;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "openedAt")
    private Instant openedAt;

    @Column(name = "openCount", nullable = false)
    private int openCount = 0;

    @Column(name = "clickCount", nullable = false)
    private int clickCount = 0;

    /**
     * Arbitrary key-value tags for filtering and correlation.
     * Auto-populated by the workflow engine: workflowId, stepRunId.
     * Stored as JSONB; add a GIN index for efficient tag-based queries:
     *   CREATE INDEX idx_email_log_tags ON email_log USING GIN (tags jsonb_path_ops);
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private Map<String, String> tags = new HashMap<>();

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "sentAt")
    private Instant sentAt;

    public EmailLog() {
    }

    public EmailLog(UUID id, UUID userId, UUID appKeyId, String fromAddress, String toAddress, String subject, EmailStatus status, EmailType emailType) {
        this.id = id;
        this.userId = userId;
        this.appKeyId = appKeyId;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.subject = subject;
        this.status = status;
        this.emailType = emailType;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getAppKeyId() {
        return appKeyId;
    }

    public void setAppKeyId(UUID appKeyId) {
        this.appKeyId = appKeyId;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public void setStatus(EmailStatus status) {
        this.status = status;
    }

    public EmailType getEmailType() { return emailType; }
    public void setEmailType(EmailType emailType) { this.emailType = emailType; }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }

    public int getOpenCount() { return openCount; }
    public void setOpenCount(int openCount) { this.openCount = openCount; }

    public int getClickCount() { return clickCount; }
    public void setClickCount(int clickCount) { this.clickCount = clickCount; }

    public Map<String, String> getTags() { return tags; }
    public void setTags(Map<String, String> tags) { this.tags = tags != null ? tags : new HashMap<>(); }
    public void addTag(String key, String value) { if (this.tags == null) this.tags = new HashMap<>(); this.tags.put(key, value); }
}
