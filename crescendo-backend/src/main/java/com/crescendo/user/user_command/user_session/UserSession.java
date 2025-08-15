package com.crescendo.user.user_command.user_session;

import com.crescendo.user.user_command.User_command;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks issued refresh tokens (hashed) and session metadata.
 */
@Entity
/// Indexes tell that when creating table create indexes for the given columns.
/// Here the selection process improves.
/// The index column is the name given, and it creates index from the column list given
@Table(name = "user_session",
        indexes = {
                @Index(name = "idx_user_session_user_id", columnList = "user_id"),
                @Index(name = "idx_user_session_expires_at", columnList = "expires_at")
        }
)
public class UserSession {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /// ManyToOne is used to map many entities to one entity
    /// FetchType.LAZY means that the referenced entity will not be loaded from the database until we actually access it
    /// optional=false means that the relationship is mandatory
    /// JoinColumn tells how relationship is mapped.
    /// Referenced column name is the name of the column of the foreign table.
    /// Foreign Key is used to explicitly name the foreign key constraint
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_session_user"))
    private User_command user;

    @Column(name = "refresh_token_hash", nullable = false, length = 100)
    private String refreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserSession() {
    }

    public UserSession(UUID id, User_command user, String refreshTokenHash, Instant expiresAt) {
        this.id = id;
        this.user = user;
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User_command getUser() {
        return user;
    }

    public void setUser(User_command user) {
        this.user = user;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
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
}
