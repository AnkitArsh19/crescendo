package com.crescendo.emailservice.suppression;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Records an email address that must not receive emails from a given user/account.
 *
 * Suppression reasons:
 *   HARD_BOUNCE            — automatically added when an email hard-bounces
 *   SOFT_BOUNCE_THRESHOLD  — automatically added when an email soft-bounces >= 3 times
 *   UNSUBSCRIBED           — added when recipient clicks the List-Unsubscribe link
 *   MANUAL                 — manually added via the management API
 *   COMPLAINED             — added when recipient marks email as spam
 *
 * All addresses are stored normalized (lowercase, trimmed) so lookups are case-insensitive.
 */
@Entity
@Table(name = "email_suppression",
        indexes = {
                @Index(name = "idx_suppression_user_email", columnList = "userId,normalizedEmail", unique = true),
                @Index(name = "idx_suppression_user", columnList = "userId")
        })
public class EmailSuppression {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 320)
    private String normalizedEmail;

    @Column(nullable = false, length = 30)
    private String reason;

    @Column(name = "consecutive_soft_bounces", nullable = false)
    private int consecutiveSoftBounces = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailSuppression() {}

    public static EmailSuppression of(UUID userId, String email, String reason) {
        EmailSuppression s = new EmailSuppression();
        s.id = UUID.randomUUID();
        s.userId = userId;
        s.normalizedEmail = email.toLowerCase().trim();
        s.reason = reason;
        return s;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public int getConsecutiveSoftBounces() { return consecutiveSoftBounces; }
    public void setConsecutiveSoftBounces(int consecutiveSoftBounces) { this.consecutiveSoftBounces = consecutiveSoftBounces; }
    public Instant getCreatedAt() { return createdAt; }
}
