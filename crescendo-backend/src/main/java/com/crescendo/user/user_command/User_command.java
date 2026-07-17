package com.crescendo.user.user_command;

import com.crescendo.enums.UserRole;
import com.crescendo.shared.domain.valueobject.Email;
import com.crescendo.shared.domain.valueobject.Username;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;

/**
 * Entity to store user details and command operations.
 * Uses value objects for Email and Username for validation and type safety.
 */
@Entity
@Table(name = "user_command",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email_id", unique = true),
        @Index(name = "idx_user_username", columnList = "username")
    })
public class User_command {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email_id", nullable = false, length = 320))
    private Email email;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "username", nullable = false, length = 100))
    private Username userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    /// Tracks whether the user has confirmed ownership of their email address.
    /// Defaults to false at registration; set to true when the verification link is consumed.
    /// OAuth-registered users may bypass this if the provider has already verified the email.
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "storage_used_bytes", nullable = false, columnDefinition = "bigint default 0")
    private Long storageUsedBytes = 0L;

    // ── Passkey nudge throttle ──────────────────────────────────────────────────
    // How many times the "set up a passkey" nudge has been dismissed (temporary X).
    // After 2 dismissals, the nudge stops appearing. An explicit opt-out sets
    // passkeyNudgeOptedOut = true immediately, overriding this counter.
    @Column(name = "passkeyNudgeDismissCount", nullable = false,
            columnDefinition = "integer default 0")
    private int passkeyNudgeDismissCount = 0;

    // Timestamp of the last temporary dismissal — used to enforce the 14-day cooldown.
    @Column(name = "passkeyNudgeLastDismissedAt")
    private Instant passkeyNudgeLastDismissedAt;

    // Set to true when the user explicitly clicks "Don't ask again".
    // Checked first — if true, the nudge never appears regardless of count/cooldown.
    @Column(name = "passkeyNudgeOptedOut", nullable = false,
            columnDefinition = "boolean default false")
    private boolean passkeyNudgeOptedOut = false;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    public User_command() {
    }

    public User_command(UUID id, Username userName, Email email, UserRole role) {
        this.id = id;
        this.userName = userName;
        this.email = email;
        this.role = role;
    }

    /**
     * Convenience constructor accepting raw strings (validates internally).
     */
    public User_command(UUID id, String userName, String email, UserRole role) {
        this(id, Username.of(userName), Email.of(email), role);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Email getEmail() {
        return email;
    }

    /**
     * Returns raw email string for compatibility.
     */
    public String getEmailId() {
        return email != null ? email.value() : null;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public void setEmailId(String emailId) {
        this.email = Email.of(emailId);
    }

    public Username getUserNameVO() {
        return userName;
    }

    /**
     * Returns raw username string for compatibility.
     */
    public String getUserName() {
        return userName != null ? userName.value() : null;
    }

    public void setUserName(Username userName) {
        this.userName = userName;
    }

    public void setUserName(String userName) {
        this.userName = Username.of(userName);
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getStorageUsedBytes() {
        return storageUsedBytes;
    }

    public void setStorageUsedBytes(Long storageUsedBytes) {
        this.storageUsedBytes = storageUsedBytes;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public int getPasskeyNudgeDismissCount() {
        return passkeyNudgeDismissCount;
    }

    public void setPasskeyNudgeDismissCount(int passkeyNudgeDismissCount) {
        this.passkeyNudgeDismissCount = passkeyNudgeDismissCount;
    }

    public Instant getPasskeyNudgeLastDismissedAt() {
        return passkeyNudgeLastDismissedAt;
    }

    public void setPasskeyNudgeLastDismissedAt(Instant passkeyNudgeLastDismissedAt) {
        this.passkeyNudgeLastDismissedAt = passkeyNudgeLastDismissedAt;
    }

    public boolean isPasskeyNudgeOptedOut() {
        return passkeyNudgeOptedOut;
    }

    public void setPasskeyNudgeOptedOut(boolean passkeyNudgeOptedOut) {
        this.passkeyNudgeOptedOut = passkeyNudgeOptedOut;
    }
}
