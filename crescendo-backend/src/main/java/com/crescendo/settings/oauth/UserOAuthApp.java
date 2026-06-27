package com.crescendo.settings.oauth;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores a user's custom OAuth app configuration for a given provider.
 *
 * <p>When a user configures their own client_id and client_secret, Crescendo will
 * use <em>their</em> OAuth app instead of the platform-level credentials when
 * initiating the authorization flow. This enables:
 * <ul>
 *   <li>Custom branding on the OAuth consent screen</li>
 *   <li>Custom scopes — the user decides exactly what permissions to request</li>
 *   <li>Higher rate limits tied to their own developer account</li>
 * </ul>
 *
 * <p><strong>Security:</strong> {@code encryptedClientId} and {@code encryptedClientSecret}
 * are encrypted with AES-256-GCM via {@link com.crescendo.connections.security.ConnectionCredentialsCryptoService}.
 * They are decrypted only at runtime when building the authorization URL.
 *
 * <p>One row per (userId, providerKey) — there is a unique constraint enforcing this.
 */
@Entity
@Table(
    name = "user_oauth_app",
    indexes = {
        @Index(name = "idx_user_oauth_app_user", columnList = "user_id"),
        @Index(name = "idx_user_oauth_app_provider", columnList = "provider_key")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_oauth_app", columnNames = {"user_id", "provider_key"})
    }
)
public class UserOAuthApp {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The OAuth provider key, e.g. "slack", "github", "gmail".
     * Matches the keys used in {@link com.crescendo.connections.oauth.IntegrationOAuthConfig}.
     */
    @Column(name = "provider_key", nullable = false, length = 100)
    private String providerKey;

    /**
     * AES-256-GCM encrypted client ID.
     * Encrypted via ConnectionCredentialsCryptoService.seal() with a single-field map.
     */
    @Column(name = "encrypted_client_id", columnDefinition = "TEXT", nullable = false)
    private String encryptedClientId;

    /**
     * AES-256-GCM encrypted client secret.
     * Encrypted via ConnectionCredentialsCryptoService.seal() with a single-field map.
     */
    @Column(name = "encrypted_client_secret", columnDefinition = "TEXT", nullable = false)
    private String encryptedClientSecret;

    /**
     * Space-separated OAuth scopes the user wants to request.
     * Example: "channels:read chat:write users:read"
     * If blank, falls back to the platform-level scopes from application.properties.
     */
    @Column(name = "scopes", columnDefinition = "TEXT")
    private String scopes;

    /**
     * Whether this custom OAuth app config is enabled.
     * When false, the platform-level OAuth app is used instead.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserOAuthApp() {}

    public UserOAuthApp(UUID userId, String providerKey,
                        String encryptedClientId, String encryptedClientSecret,
                        String scopes) {
        this.userId = userId;
        this.providerKey = providerKey;
        this.encryptedClientId = encryptedClientId;
        this.encryptedClientSecret = encryptedClientSecret;
        this.scopes = scopes;
    }

    // ── Getters / Setters ─────────────────────────────────────────

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getProviderKey() { return providerKey; }
    public void setProviderKey(String providerKey) { this.providerKey = providerKey; }

    public String getEncryptedClientId() { return encryptedClientId; }
    public void setEncryptedClientId(String encryptedClientId) { this.encryptedClientId = encryptedClientId; }

    public String getEncryptedClientSecret() { return encryptedClientSecret; }
    public void setEncryptedClientSecret(String encryptedClientSecret) { this.encryptedClientSecret = encryptedClientSecret; }

    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
