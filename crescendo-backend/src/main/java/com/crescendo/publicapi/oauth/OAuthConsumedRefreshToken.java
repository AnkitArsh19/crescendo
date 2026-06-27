package com.crescendo.publicapi.oauth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "oauth2_consumed_refresh_token", indexes = {
        @Index(name = "idx_consumed_refresh_authorization", columnList = "authorization_id"),
        @Index(name = "idx_consumed_refresh_expires", columnList = "expires_at")
})
public class OAuthConsumedRefreshToken {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "authorization_id", nullable = false, length = 100)
    private String authorizationId;

    @Column(name = "registered_client_id", nullable = false, length = 100)
    private String registeredClientId;

    @Column(name = "principal_name", nullable = false, length = 200)
    private String principalName;

    @CreationTimestamp
    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected OAuthConsumedRefreshToken() {
    }

    public OAuthConsumedRefreshToken(
            String tokenHash,
            String authorizationId,
            String registeredClientId,
            String principalName,
            Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.authorizationId = authorizationId;
        this.registeredClientId = registeredClientId;
        this.principalName = principalName;
        this.expiresAt = expiresAt;
    }

    public String getAuthorizationId() {
        return authorizationId;
    }
}
