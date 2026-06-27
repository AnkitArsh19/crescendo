package com.crescendo.settings.oauth;

import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages per-user custom OAuth app configurations.
 *
 * <p>Encrypts client_id and client_secret using AES-256-GCM before persisting them
 * as JSON strings in the TEXT column. Decrypts on demand when building authorization URLs.
 *
 * <p>Seal/unseal uses the same {@link ConnectionCredentialsCryptoService} as user connections.
 */
@Service
public class UserOAuthAppService {

    private final UserOAuthAppRepository repo;
    private final ConnectionCredentialsCryptoService crypto;
    private final ObjectMapper objectMapper;

    public UserOAuthAppService(UserOAuthAppRepository repo,
                               ConnectionCredentialsCryptoService crypto,
                               ObjectMapper objectMapper) {
        this.repo = repo;
        this.crypto = crypto;
        this.objectMapper = objectMapper;
    }

    // ── Public API ────────────────────────────────────────────────

    /**
     * Lists all custom OAuth apps configured by the user.
     * Secrets are NOT returned — only safe metadata.
     */
    public List<UserOAuthAppDto.OAuthAppSummary> list(UUID userId) {
        return repo.findByUserId(userId)
                .stream()
                .map(a -> new UserOAuthAppDto.OAuthAppSummary(
                        a.getProviderKey(),
                        a.getScopes(),
                        a.isEnabled(),
                        a.getCreatedAt(),
                        a.getUpdatedAt()
                ))
                .toList();
    }

    /**
     * Upserts a custom OAuth app config for the given provider.
     * Encrypts client_id and client_secret before persisting.
     */
    @Transactional
    public void save(UUID userId, UserOAuthAppDto.SaveOAuthAppRequest req) {
        if (req.clientId() == null || req.clientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientId is required");
        }
        if (req.clientSecret() == null || req.clientSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientSecret is required");
        }

        String sealedClientId = sealToJson(Map.of("v", req.clientId()));
        String sealedClientSecret = sealToJson(Map.of("v", req.clientSecret()));

        UserOAuthApp app = repo.findByUserIdAndProviderKey(userId, req.providerKey())
                .orElse(new UserOAuthApp());

        app.setUserId(userId);
        app.setProviderKey(req.providerKey());
        app.setEncryptedClientId(sealedClientId);
        app.setEncryptedClientSecret(sealedClientSecret);
        app.setScopes(req.scopes());
        app.setEnabled(true);

        repo.save(app);
    }

    /**
     * Deletes the custom OAuth app config for a specific provider.
     */
    @Transactional
    public void delete(UUID userId, String providerKey) {
        repo.findByUserIdAndProviderKey(userId, providerKey)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No custom OAuth app configured for provider: " + providerKey));
        repo.deleteByUserIdAndProviderKey(userId, providerKey);
    }

    /**
     * Returns decrypted credentials if the user has an enabled custom OAuth app for the provider.
     * Returns {@code null} if none configured — caller falls back to platform credentials.
     *
     * Called by {@link com.crescendo.connections.oauth.IntegrationOAuthService} before
     * falling back to platform-level credentials in application.properties.
     */
    public UserOAuthAppDto.DecryptedOAuthApp getDecrypted(UUID userId, String providerKey) {
        return repo.findByUserIdAndProviderKey(userId, providerKey)
                .filter(UserOAuthApp::isEnabled)
                .map(app -> {
                    try {
                        Map<String, Object> decryptedId = crypto.open(fromJson(app.getEncryptedClientId()));
                        Map<String, Object> decryptedSecret = crypto.open(fromJson(app.getEncryptedClientSecret()));
                        return new UserOAuthAppDto.DecryptedOAuthApp(
                                (String) decryptedId.get("v"),
                                (String) decryptedSecret.get("v"),
                                app.getScopes()
                        );
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    // ── Crypto helpers ────────────────────────────────────────────

    private String sealToJson(Map<String, Object> plainMap) {
        try {
            Map<String, Object> sealed = crypto.seal(plainMap);
            return objectMapper.writeValueAsString(sealed);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to encrypt OAuth app credentials");
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse OAuth app credentials");
        }
    }
}
