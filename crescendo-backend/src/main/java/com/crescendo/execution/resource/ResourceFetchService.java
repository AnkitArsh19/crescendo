package com.crescendo.execution.resource;

import com.crescendo.admin.PlatformKey;
import com.crescendo.admin.PlatformKeyRepository;
import com.crescendo.connections.connections_command.Connections_command;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.oauth.OAuthTokenRefreshService;
import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import tools.jackson.databind.ObjectMapper;

import com.crescendo.security.AppUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates dynamic resource fetching for workflow configuration.
 * <p>
 * Flow:
 * <ol>
 *   <li>Validates user owns the connection</li>
 *   <li>Gets valid (auto-refreshed) credentials via {@link OAuthTokenRefreshService}</li>
 *   <li>Delegates to the app's {@link ResourceProvider}</li>
 *   <li>Returns selectable options to the frontend</li>
 * </ol>
 * <p>
 * Also supports {@code ADMIN_KEY} mode where no user connection exists — the platform-wide
 * key stored in {@link PlatformKey} is used instead (e.g. Crescendo's own Telegram bot token).
 */
@Service
public class ResourceFetchService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceFetchService.class);

    private final ResourceProviderRegistry registry;
    private final Connections_commandRepository connectionsRepo;
    private final OAuthTokenRefreshService tokenService;
    private final PlatformKeyRepository platformKeyRepo;
    private final ConnectionCredentialsCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${telegram.bot.token:}")
    private String telegramBotToken;

    @org.springframework.beans.factory.annotation.Value("${gemini.api.key:}")
    private String geminiApiKey;

    @org.springframework.beans.factory.annotation.Value("${sarvam.api.key:}")
    private String sarvamApiKey;

    public ResourceFetchService(ResourceProviderRegistry registry,
                                 Connections_commandRepository connectionsRepo,
                                 OAuthTokenRefreshService tokenService,
                                 PlatformKeyRepository platformKeyRepo,
                                 ConnectionCredentialsCryptoService cryptoService,
                                 ObjectMapper objectMapper) {
        this.registry = registry;
        this.connectionsRepo = connectionsRepo;
        this.tokenService = tokenService;
        this.platformKeyRepo = platformKeyRepo;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches dynamic resources for a given app/connection/resource type.
     */
    public List<ResourceOption> fetchResources(String appKey, String resourceType,
                                                UUID connectionId, UUID userId,
                                                Map<String, String> params) {
        // 1. Resolve provider
        ResourceProvider provider = registry.find(appKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No resource provider registered for app: " + appKey));

        if (!provider.supportedResourceTypes().contains(resourceType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Resource type '" + resourceType + "' is not supported by " + appKey
                    + ". Supported: " + provider.supportedResourceTypes());
        }

        // 2. Load and validate connection ownership
        if (connectionId == null) {
            try {
                List<ResourceOption> fallback = provider.listResources(Map.of(), resourceType, params);
                if (fallback != null && !fallback.isEmpty()) {
                    return fallback;
                }
            } catch (Exception ignored) {}
            return List.of();
        }

        Connections_command connection = connectionsRepo.findByIdAndUser_Id(connectionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Connection not found or access denied"));

        // 3. Verify connection belongs to the right app
        if (!appKey.equals(connection.getAppKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Connection belongs to app '" + connection.getAppKey()
                    + "', not '" + appKey + "'");
        }

        // 4. Get valid (auto-refreshed) credentials
        Map<String, Object> credentials = tokenService.getValidCredentials(connection);

        // 5. Fetch from external API via provider
        return callProvider(provider, credentials, appKey, resourceType, params);
    }

    /**
     * Fetches dynamic resources using the platform's own admin key (no user connection required).
     * Used when {@code credentialSource == ADMIN_KEY} — e.g. Crescendo's own Telegram bot.
     */
    public List<ResourceOption> fetchResourcesWithAdminKey(String appKey, String resourceType,
                                                            UUID userId,
                                                            Map<String, String> params) {
        // 1. Resolve provider
        ResourceProvider provider = registry.find(appKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No resource provider registered for app: " + appKey));

        if (!provider.supportedResourceTypes().contains(resourceType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Resource type '" + resourceType + "' is not supported by " + appKey
                    + ". Supported: " + provider.supportedResourceTypes());
        }

        // 2. Load the platform key credentials (DB first, fallback to application.properties)
        Map<String, Object> credentials = null;
        try {
            PlatformKey pk = platformKeyRepo.findByAppKeyAndEnabledTrue(appKey).orElse(null);
            if (pk != null && pk.getEncryptedCredentials() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sealed = objectMapper.readValue(pk.getEncryptedCredentials(), Map.class);
                credentials = cryptoService.open(sealed);
            }
        } catch (Exception e) {
            logger.warn("[resources] Failed to decrypt platform key from DB for app '{}': {}", appKey, e.getMessage());
        }

        if (credentials == null || credentials.isEmpty()) {
            if ("telegram".equalsIgnoreCase(appKey) && telegramBotToken != null && !telegramBotToken.isBlank()) {
                credentials = Map.of("apiKey", telegramBotToken, "botToken", telegramBotToken);
            } else if ("gemini".equalsIgnoreCase(appKey) && geminiApiKey != null && !geminiApiKey.isBlank()) {
                credentials = Map.of("apiKey", geminiApiKey);
            } else if ("sarvam".equalsIgnoreCase(appKey) && sarvamApiKey != null && !sarvamApiKey.isBlank()) {
                credentials = Map.of("apiKey", sarvamApiKey);
            }
        }

        if (credentials == null || credentials.isEmpty()) {
            // Check if provider can serve standard options (e.g. YouTube videoCategories) without credentials
            try {
                List<ResourceOption> fallback = provider.listResources(Map.of(), resourceType, params);
                if (fallback != null && !fallback.isEmpty()) {
                    return fallback;
                }
            } catch (Exception ignored) {}

            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No platform key configured for app: " + appKey);
        }

        logger.debug("[resources] Using PLATFORM credentials for app='{}' resource='{}' (ADMIN_KEY mode)", appKey, resourceType);
        return callProvider(provider, credentials, appKey, resourceType, params);
    }

    private List<ResourceOption> callProvider(ResourceProvider provider, Map<String, Object> credentials,
                                               String appKey, String resourceType, Map<String, String> params) {
        try {
            logger.debug("[resources] Fetching {} for app={}", resourceType, appKey);
            List<ResourceOption> options = provider.listResources(credentials, resourceType, params);
            logger.debug("[resources] Returned {} option(s) for {}:{}", options.size(), appKey, resourceType);
            return options;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[resources] Failed to fetch {} for app={}: {}", resourceType, appKey, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to fetch resources from " + appKey + ": " + e.getMessage());
        }
    }

    /**
     * Extracts the user ID from the Spring Security authentication object.
     */
    public UUID extractUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return details.getId();
    }
}

