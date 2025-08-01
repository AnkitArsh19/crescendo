package com.crescendo.emailservice.apikey.key_query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "apikey_query")
public class ApiKey_query {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "prefix", nullable = false)
    private String prefix;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "revokedAt")
    private Instant revokedAt;

    @Column(name = "lastUsedAt")
    private Instant lastUsedAt;

    public ApiKey_query() {
    }

    public ApiKey_query(UUID id, String name, String prefix, Instant revokedAt, Instant createdAt, Instant lastUsedAt) {
        this.id = id;
        this.name = name;
        this.prefix = prefix;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }
}

