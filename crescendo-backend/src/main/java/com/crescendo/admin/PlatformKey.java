package com.crescendo.admin;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin-configured platform-wide API keys for apps.
 *
 * When a user doesn't have their own connection for an API-key-based app,
 * the platform key can be used if available and the admin has enabled it.
 *
 * Credentials are stored encrypted using the same crypto service as user connections.
 */
@Entity
@Table(name = "platform_key", indexes = {
        @Index(name = "idx_platform_key_app", columnList = "app_key", unique = true)
})
public class PlatformKey {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "app_key", nullable = false, unique = true, length = 100)
    private String appKey;

    @Column(name = "app_name", length = 200)
    private String appName;

    /**
     * Encrypted credentials JSON string.
     * Uses the same ConnectionCredentialsCryptoService as user connections.
     */
    @Column(name = "encrypted_credentials", columnDefinition = "TEXT")
    private String encryptedCredentials;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "added_by", length = 320)
    private String addedBy;

    /** Tracks total usage count for analytics */
    @Column(name = "usage_count", nullable = false)
    private long usageCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Indicates which master key version was used to encrypt {@code encryptedCredentials}.
     * Defaults to 1. Increment when re-encrypting with a rotated master key.
     */
    @Column(name = "key_version", nullable = false)
    private int keyVersion = 1;

    public PlatformKey() {}

    public PlatformKey(String appKey, String appName, String encryptedCredentials, String addedBy) {
        this.appKey = appKey;
        this.appName = appName;
        this.encryptedCredentials = encryptedCredentials;
        this.addedBy = addedBy;
    }

    public UUID getId() { return id; }
    public String getAppKey() { return appKey; }
    public String getAppName() { return appName; }
    public String getEncryptedCredentials() { return encryptedCredentials; }
    public void setEncryptedCredentials(String encryptedCredentials) { this.encryptedCredentials = encryptedCredentials; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAddedBy() { return addedBy; }
    public long getUsageCount() { return usageCount; }
    public void incrementUsageCount() { this.usageCount++; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getKeyVersion() { return keyVersion; }
    public void setKeyVersion(int keyVersion) { this.keyVersion = keyVersion; }
}
