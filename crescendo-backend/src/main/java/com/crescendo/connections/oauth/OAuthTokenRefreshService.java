package com.crescendo.connections.oauth;

import com.crescendo.connections.connections_command.Connections_command;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import com.crescendo.shared.infrastructure.lock.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Ensures OAuth access tokens are valid before use.
 *
 * <p>Flow per connection:
 * <ol>
 *   <li>Decrypt stored credentials</li>
 *   <li>Check if {@code tokenExpiresAt} is in the future (with 5-minute buffer)</li>
 *   <li>If expired, acquire distributed lock → refresh via provider's token URL → re-encrypt</li>
 *   <li>Return the valid (possibly refreshed) credentials</li>
 * </ol>
 *
 * <p>Uses {@link DistributedLockService} so concurrent requests don't race on refresh.
 */
@Service
public class OAuthTokenRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenRefreshService.class);

    /** Refresh 5 minutes before actual expiry to avoid edge-case 401s. */
    private static final long EXPIRY_BUFFER_SECONDS = 300;

    /** Lock TTL — token refresh should never take longer than 30 seconds. */
    private static final long LOCK_TTL_MS = 30_000;

    private final Connections_commandRepository connectionRepo;
    private final ConnectionCredentialsCryptoService cryptoService;
    private final IntegrationOAuthConfig oauthConfig;
    private final DistributedLockService lockService;
    private final RestTemplate restTemplate;

    public OAuthTokenRefreshService(Connections_commandRepository connectionRepo,
                                     ConnectionCredentialsCryptoService cryptoService,
                                     IntegrationOAuthConfig oauthConfig,
                                     DistributedLockService lockService) {
        this.connectionRepo = connectionRepo;
        this.cryptoService = cryptoService;
        this.oauthConfig = oauthConfig;
        this.lockService = lockService;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Returns valid (non-expired) credentials for a connection.
     * If the access token is expired and a refresh token exists, it will be refreshed transparently.
     *
     * @param connectionId the connection ID
     * @param userId       authenticated user's ID (ownership check)
     * @return decrypted credentials with a valid access token
     */
    public Map<String, Object> getValidCredentials(UUID connectionId, UUID userId) {
        Connections_command connection = connectionRepo.findByIdAndUser_Id(connectionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Connection not found or access denied"));

        Map<String, Object> credentials = cryptoService.open(connection.getCredentials());

        // API-key connections (bot tokens, secret keys) never expire
        if (!credentials.containsKey("accessToken")) {
            return credentials;
        }

        // Check expiry
        if (!isTokenExpired(credentials)) {
            return credentials;
        }

        // Token is expired — attempt refresh
        String refreshToken = asString(credentials.get("refreshToken"));
        if (refreshToken == null || refreshToken.isBlank()) {
            logger.warn("[token-refresh] Token expired but no refresh token for connection {}", connectionId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Access token expired and no refresh token available. Please reconnect the app.");
        }

        return refreshAndStore(connection, credentials, refreshToken);
    }

    /**
     * Overload that skips ownership check — used internally by execution engine
     * where ownership was already validated upstream.
     */
    public Map<String, Object> getValidCredentials(Connections_command connection) {
        Map<String, Object> credentials = cryptoService.open(connection.getCredentials());

        if (!credentials.containsKey("accessToken")) {
            return credentials;
        }

        if (!isTokenExpired(credentials)) {
            return credentials;
        }

        String refreshToken = asString(credentials.get("refreshToken"));
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Access token expired. Please reconnect.");
        }

        return refreshAndStore(connection, credentials, refreshToken);
    }

    // ─── Core Refresh Logic ──────────────────────────────────────────

    private Map<String, Object> refreshAndStore(Connections_command connection,
                                                  Map<String, Object> currentCredentials,
                                                  String refreshToken) {
        UUID connectionId = connection.getId();
        String lockKey = "token-refresh:" + connectionId;

        Optional<String> lockToken = lockService.tryLock(lockKey, LOCK_TTL_MS);
        if (lockToken.isEmpty()) {
            // Another thread is refreshing. Wait briefly, then re-read from DB.
            logger.debug("[token-refresh] Lock held by another thread, waiting for connection {}", connectionId);
            try { Thread.sleep(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

            // Re-read — the other thread should have refreshed by now
            Connections_command refreshed = connectionRepo.findById(connectionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found"));
            return cryptoService.open(refreshed.getCredentials());
        }

        try {
            // Double-check after acquiring lock (another thread may have just refreshed)
            Connections_command latest = connectionRepo.findById(connectionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found"));
            Map<String, Object> latestCreds = cryptoService.open(latest.getCredentials());

            if (!isTokenExpired(latestCreds)) {
                return latestCreds; // Already refreshed by another thread
            }

            // Resolve provider config
            String appKey = connection.getAppKey();
            String providerKey = resolveProviderKey(appKey);

            if (!oauthConfig.hasProvider(providerKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No OAuth provider configured for: " + providerKey);
            }

            IntegrationOAuthConfig.ProviderConfig config = oauthConfig.getProvider(providerKey);

            // Exchange refresh token for new access token
            Map<String, Object> tokenResponse = exchangeRefreshToken(config, refreshToken, providerKey);

            // Build updated credentials
            Map<String, Object> updated = new HashMap<>(latestCreds);
            updated.put("accessToken", tokenResponse.get("access_token"));

            // Some providers rotate refresh tokens (Microsoft does, Google usually doesn't)
            if (tokenResponse.containsKey("refresh_token")) {
                updated.put("refreshToken", tokenResponse.get("refresh_token"));
            }

            // Compute absolute expiry time
            Object expiresIn = tokenResponse.get("expires_in");
            if (expiresIn != null) {
                long seconds = expiresIn instanceof Number n ? n.longValue() : Long.parseLong(expiresIn.toString());
                updated.put("expiresIn", seconds);
                updated.put("tokenExpiresAt", Instant.now().plusSeconds(seconds).toString());
            }

            // Re-encrypt and persist
            Map<String, Object> encrypted = cryptoService.seal(updated);
            latest.setCredentials(encrypted);
            connectionRepo.save(latest);

            logger.info("[token-refresh] Refreshed token for connection {} (app={})", connectionId, appKey);
            return updated;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[token-refresh] Refresh failed for connection {}: {}", connectionId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Token refresh failed: " + e.getMessage());
        } finally {
            lockService.unlock(lockKey, lockToken.get());
        }
    }

    // ─── Token Exchange ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeRefreshToken(IntegrationOAuthConfig.ProviderConfig config,
                                                       String refreshToken,
                                                       String providerKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // Notion and Reddit require Basic auth for token exchange
        if (isBasicAuthProvider(providerKey)) {
            String basic = Base64.getEncoder().encodeToString(
                    (config.getClientId() + ":" + config.getClientSecret()).getBytes(StandardCharsets.UTF_8)
            );
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basic);
        }

        // Reddit requires a custom User-Agent
        if ("reddit".equals(providerKey)) {
            headers.set("User-Agent", "crescendo:v1.0 (by /u/crescendo-app)");
        }

        // Most providers use form-urlencoded
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        if (!isBasicAuthProvider(providerKey)) {
            body.add("client_id", config.getClientId());
            body.add("client_secret", config.getClientSecret());
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    config.getTokenUrl(), HttpMethod.POST, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Token refresh returned non-200: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e) {
            logger.error("[token-refresh] Exchange failed for provider {}: {}", providerKey, e.getMessage());
            throw new RuntimeException("Failed to refresh token for " + providerKey + ": " + e.getMessage(), e);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private boolean isTokenExpired(Map<String, Object> credentials) {
        Object expiresAtObj = credentials.get("tokenExpiresAt");
        if (expiresAtObj == null) {
            // Legacy connections without tokenExpiresAt — assume expired if older than 1 hour
            // This forces a refresh which will then store tokenExpiresAt going forward
            return true;
        }

        try {
            Instant expiresAt = Instant.parse(expiresAtObj.toString());
            return Instant.now().isAfter(expiresAt.minusSeconds(EXPIRY_BUFFER_SECONDS));
        } catch (Exception e) {
            logger.warn("[token-refresh] Could not parse tokenExpiresAt: {}", expiresAtObj);
            return true;
        }
    }

    private boolean isBasicAuthProvider(String providerKey) {
        return "notion".equals(providerKey) || "reddit".equals(providerKey);
    }

    /**
     * Maps app key → OAuth provider key. Most are 1:1, but Google apps map separately.
     */
    private String resolveProviderKey(String appKey) {
        return switch (appKey) {
            case "gmail" -> "gmail";
            case "google-sheets" -> "google-sheets";
            case "google-drive" -> "google-drive";
            case "google-calendar" -> "google-calendar";
            case "google-forms" -> "google-forms";
            case "google-tasks" -> "google-tasks";
            case "google-slides" -> "google-slides";
            case "google-docs" -> "google-docs";
            default -> appKey;
        };
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
