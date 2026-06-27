package com.crescendo.emailservice.apikey.key_query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-side projection for API keys.
 * No hashed key — only metadata visible on the query side.
 */
@Entity
@Table(name = "apikey_query",
    indexes = {
        @Index(name = "idx_apikey_query_user", columnList = "user_id")
    })
public class ApiKey_query {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

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

    public ApiKey_query() {
    }

    public ApiKey_query(UUID id, UUID userId, String name, String prefix) {
        this(id, userId, name, prefix, "", 100, null);
    }

    public ApiKey_query(UUID id, UUID userId, String name, String prefix, String scopes,
                        int rateLimitPerMinute, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.prefix = prefix;
        this.scopes = scopes == null ? "" : scopes;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
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

    public Instant getCreatedAt() {
        return createdAt;
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

    public void setName(String name) {
        this.name = name;
    }
}

