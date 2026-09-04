package com.crescendo.notification.preference;

import com.crescendo.notification.NotificationType;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Stores a user's per-type notification preference.
 * If no row exists for a (userId, type) pair, the default is enabled (true).
 */
@Entity
@Table(
    name = "notification_preference",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_notif_pref_user_type",
        columnNames = {"userId", "type"}
    )
)
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private NotificationType type;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected NotificationPreference() {}

    public NotificationPreference(UUID userId, NotificationType type) {
        this.userId = userId;
        this.type = type;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
