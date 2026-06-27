package com.crescendo.publicapi.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_key_usage_log",
        indexes = {
                @Index(name = "idx_api_key_usage_key_time", columnList = "api_key_id,createdAt"),
                @Index(name = "idx_api_key_usage_user_time", columnList = "user_id,createdAt")
        })
public class ApiKeyUsageLog {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "api_key_id", nullable = false)
    private UUID apiKeyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "method", nullable = false, length = 12)
    private String method;

    @Column(name = "path", nullable = false, length = 1000)
    private String path;

    @Column(name = "status", nullable = false)
    private int status;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    public ApiKeyUsageLog() {}

    public ApiKeyUsageLog(UUID id, UUID apiKeyId, UUID userId, String method, String path,
                          int status, String ipAddress, String userAgent) {
        this.id = id;
        this.apiKeyId = apiKeyId;
        this.userId = userId;
        this.method = method;
        this.path = path;
        this.status = status;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApiKeyId() {
        return apiKeyId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public int getStatus() {
        return status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
