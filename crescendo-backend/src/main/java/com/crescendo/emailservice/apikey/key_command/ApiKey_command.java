package com.crescendo.emailservice.apikey.key_command;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
/// Indexes tell that when creating table create indexes for the given columns.
/// Here the selection process improves.
/// The index column is the name given, and it creates index from the column list given
@Table(name = "apikey_command",
    indexes = {
        @Index(name = "idx_apikey_user", columnList = "user_id"),
        @Index(name = "idx_apikey_prefix", columnList = "prefix")
    })
public class ApiKey_command {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "hashedKey", nullable = false)
    private String hashedKey;

    @Column(name = "prefix", nullable = false)
    private String prefix;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "revokedAt")
    private Instant revokedAt;

    @Column(name = "lastUsedAt")
    private Instant lastUsedAt;

    public ApiKey_command() {
    }

    public ApiKey_command(UUID userId, UUID id, String name, String hashedKey, String prefix, Instant createdAt, Instant revokedAt, Instant lastUsedAt) {
        this.userId = userId;
        this.id = id;
        this.name = name;
        this.hashedKey = hashedKey;
        this.prefix = prefix;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
        this.lastUsedAt = lastUsedAt;
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
