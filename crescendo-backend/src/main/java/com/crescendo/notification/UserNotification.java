package com.crescendo.notification;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent in-app notification for a user.
 * Survives page reload, visible from any device/tab.
 */
@Entity
@Table(
    name = "user_notification",
    indexes = {
        @Index(name = "idx_user_notification_user_unread",
               columnList = "userId, isRead, createdAt")
    }
)
public class UserNotification {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /**
     * Arbitrary context for deep-links: workflowId, runId, connectionId, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "isRead", nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserNotification() {}

    public UserNotification(UUID id, UUID userId, NotificationType type,
                            String title, String body, Map<String, Object> metadata) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.metadata = metadata;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Map<String, Object> getMetadata() { return metadata; }
    public boolean isRead() { return isRead; }
    public Instant getCreatedAt() { return createdAt; }

    // ── Mutators ──────────────────────────────────────────────────────────────

    public void markRead() { this.isRead = true; }
}
