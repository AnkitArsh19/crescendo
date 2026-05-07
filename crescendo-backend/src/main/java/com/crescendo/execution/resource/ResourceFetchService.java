package com.crescendo.execution.resource;

import com.crescendo.connections.connections_command.Connections_command;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.oauth.OAuthTokenRefreshService;
import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
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
 */
@Service
public class ResourceFetchService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceFetchService.class);

    private final ResourceProviderRegistry registry;
    private final Connections_commandRepository connectionsRepo;
    private final OAuthTokenRefreshService tokenService;
    private final ConnectionCredentialsCryptoService cryptoService;

    public ResourceFetchService(ResourceProviderRegistry registry,
                                 Connections_commandRepository connectionsRepo,
                                 OAuthTokenRefreshService tokenService,
                                 ConnectionCredentialsCryptoService cryptoService) {
        this.registry = registry;
        this.connectionsRepo = connectionsRepo;
        this.tokenService = tokenService;
        this.cryptoService = cryptoService;
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
        try {
            logger.debug("[resources] Fetching {} for app={} connection={}",
                    resourceType, appKey, connectionId);

            List<ResourceOption> options = provider.listResources(credentials, resourceType, params);

            logger.debug("[resources] Returned {} option(s) for {}:{}", 
                    options.size(), appKey, resourceType);
            return options;

        } catch (ResponseStatusException e) {
            throw e; // Re-throw HTTP errors from provider
        } catch (Exception e) {
            logger.error("[resources] Failed to fetch {} for app={}: {}",
                    resourceType, appKey, e.getMessage(), e);
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
