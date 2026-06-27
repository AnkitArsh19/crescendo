package com.crescendo.emailservice.apikey.key_command;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Table to store the secret API key generated for the email service for users to use in their software
 * This key authenticates requests coming from other servers
 * This for machine to machine communication
 */
@Entity
@Table(name = "apikey_command",
    indexes = {
        @Index(name = "idx_apikey_user", columnList = "user_id"),
        @Index(name = "idx_apikey_prefix", columnList = "prefix")
    })
public class ApiKey_command {

    /**
     * UUID for generating random ID's without interacting with the database.
     * This keeps the system secure and hard to scrape data.
     */
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Matches the user id with the api key
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Stores the hashed value of API key and not the raw key
     */
    @Column(name = "hashedKey", nullable = false, length = 100)
    private String hashedKey;

    /**
     * A short prefix for every API key to store it with the key for fast lookups
     */
    @Column(name = "prefix", nullable = false, length = 20)
    private String prefix;

    @Column(name = "scopes", length = 1000)
    private String scopes;

    @Column(name = "rateLimitPerMinute")
    private Integer rateLimitPerMinute = 100;

    @Column(name = "expiresAt")
    private Instant expiresAt;

    @Column(name = "rotationGraceEndsAt")
    private Instant rotationGraceEndsAt;

    @Column(name = "replacedByKeyId")
    private UUID replacedByKeyId;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "revokedAt")
    private Instant revokedAt;

    @Column(name = "lastUsedAt")
    private Instant lastUsedAt;

    public ApiKey_command() {
    }

    public ApiKey_command(UUID id, UUID userId, String name, String hashedKey, String prefix) {
        this(id, userId, name, hashedKey, prefix, "", 100, null);
    }

    public ApiKey_command(UUID id, UUID userId, String name, String hashedKey, String prefix,
                          String scopes, int rateLimitPerMinute, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.hashedKey = hashedKey;
        this.prefix = prefix;
        this.scopes = scopes == null ? "" : scopes;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.expiresAt = expiresAt;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHashedKey() {
        return hashedKey;
    }

    public void setHashedKey(String hashedKey) {
        this.hashedKey = hashedKey;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute == null ? 100 : rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRotationGraceEndsAt() {
        return rotationGraceEndsAt;
    }

    public void setRotationGraceEndsAt(Instant rotationGraceEndsAt) {
        this.rotationGraceEndsAt = rotationGraceEndsAt;
    }

    public UUID getReplacedByKeyId() {
        return replacedByKeyId;
    }

    public void setReplacedByKeyId(UUID replacedByKeyId) {
        this.replacedByKeyId = replacedByKeyId;
    }

    public boolean isUsableAt(Instant now) {
        if (revokedAt != null || (expiresAt != null && !expiresAt.isAfter(now))) {
            return false;
        }
        return rotationGraceEndsAt == null || rotationGraceEndsAt.isAfter(now);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
