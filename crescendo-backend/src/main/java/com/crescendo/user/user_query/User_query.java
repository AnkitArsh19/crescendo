package com.crescendo.user.user_query;

import com.crescendo.enums.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * User query table for read operations
 */
@Entity
@Table(name = "user_query",
    indexes = {
        @Index(name = "idx_user_query_email", columnList = "email_id"),
        @Index(name = "idx_user_query_username", columnList = "username")
    })
public class User_query {

    /**
     * UUID for generating random ID's without interacting with the database.
     * This keeps the system secure and hard to scrape data.
     */
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email_id", nullable = false, length = 320)
    private String emailId;

    @Column(name = "username", nullable = false, length = 100)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "providers", columnDefinition = "json", nullable = false)
    private List<String> providers;

    @Column(name = "has_local_credential", nullable = false)
    private boolean hasLocalCredential;

    /// Mirrors the email_verified flag from the command side.
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "storage_used_bytes", nullable = false, columnDefinition = "bigint default 0")
    private Long storageUsedBytes = 0L;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    public User_query() {
    }

    public User_query(UUID id, String emailId, String userName, UserRole role, List<String> providers, boolean hasLocalCredential, boolean emailVerified) {
        this.id = id;
        this.emailId = emailId;
        this.userName = userName;
        this.role = role;
        this.providers = providers;
        this.hasLocalCredential = hasLocalCredential;
        this.emailVerified = emailVerified;
    }

    public UUID getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<String> getProviders() { return providers; }

    public boolean isHasLocalCredential() { return hasLocalCredential; }

    public boolean isEmailVerified() { return emailVerified; }

    public String getEmailId() { return emailId; }

    public void setUserName(String userName) { this.userName = userName; }

    public void setRole(UserRole role) { this.role = role; }

    public void setProviders(List<String> providers) { this.providers = providers; }

    public void setHasLocalCredential(boolean hasLocalCredential) { this.hasLocalCredential = hasLocalCredential; }

    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public Long getStorageUsedBytes() { return storageUsedBytes; }

    public void setStorageUsedBytes(Long storageUsedBytes) { this.storageUsedBytes = storageUsedBytes; }
}
