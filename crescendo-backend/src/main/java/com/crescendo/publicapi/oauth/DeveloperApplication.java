package com.crescendo.publicapi.oauth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "developer_application", indexes = {
        @Index(name = "idx_developer_application_owner", columnList = "owner_user_id"),
        @Index(name = "idx_developer_application_client", columnList = "client_id", unique = true)
})
public class DeveloperApplication {
    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "client_id", nullable = false, unique = true, length = 100)
    private String clientId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "public_client", nullable = false)
    private boolean publicClient;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "rate_limit_per_minute", nullable = false)
    private int rateLimitPerMinute = 300;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeveloperApplication() {
    }

    public DeveloperApplication(
            String id,
            UUID ownerUserId,
            String clientId,
            String name,
            String logoUrl,
            boolean publicClient) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.clientId = clientId;
        this.name = name;
        this.logoUrl = logoUrl;
        this.publicClient = publicClient;
    }

    public String getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getClientId() { return clientId; }
    public String getName() { return name; }
    public String getLogoUrl() { return logoUrl; }
    public boolean isPublicClient() { return publicClient; }
    public boolean isActive() { return active; }
    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public void setActive(boolean active) { this.active = active; }
}
