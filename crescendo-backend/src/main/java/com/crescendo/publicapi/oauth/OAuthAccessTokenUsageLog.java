package com.crescendo.publicapi.oauth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "oauth_access_token_usage_log", indexes = {
        @Index(name = "idx_oauth_usage_app_created", columnList = "application_id,created_at"),
        @Index(name = "idx_oauth_usage_user", columnList = "user_id")
})
public class OAuthAccessTokenUsageLog {
    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false, length = 100)
    private String applicationId;

    @Column(name = "authorization_id", nullable = false, length = 100)
    private String authorizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 12)
    private String method;

    @Column(nullable = false, length = 1000)
    private String path;

    @Column(nullable = false)
    private int status;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OAuthAccessTokenUsageLog() {
    }

    public OAuthAccessTokenUsageLog(
            UUID id,
            String applicationId,
            String authorizationId,
            UUID userId,
            String method,
            String path,
            int status,
            String ipAddress,
            String userAgent) {
        this.id = id;
        this.applicationId = applicationId;
        this.authorizationId = authorizationId;
        this.userId = userId;
        this.method = method;
        this.path = path;
        this.status = status;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public UUID getId() { return id; }
    public String getApplicationId() { return applicationId; }
    public String getAuthorizationId() { return authorizationId; }
    public UUID getUserId() { return userId; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public int getStatus() { return status; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getCreatedAt() { return createdAt; }
}
