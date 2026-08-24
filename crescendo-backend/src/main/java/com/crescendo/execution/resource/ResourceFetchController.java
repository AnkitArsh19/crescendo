package com.crescendo.execution.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for dynamic resource fetching — powers the Zapier-style
 * cascading dropdowns in the workflow configuration panel.
 * <p>
 * Endpoint: {@code GET /apps/{appKey}/resources/{resourceType}}
 * <p>
 * Query parameters:
 * <ul>
 *   <li>{@code connectionId} (required) — which connection's credentials to use</li>
 *   <li>Any additional params are passed as parent selections for cascading
 *       (e.g. {@code ?spreadsheetId=abc&sheetName=Sheet1})</li>
 * </ul>
 */
@RestController
@RequestMapping("/apps/{appKey}/resources")
public class ResourceFetchController {

    private final ResourceFetchService resourceFetchService;

    public ResourceFetchController(ResourceFetchService resourceFetchService) {
        this.resourceFetchService = resourceFetchService;
    }

    @GetMapping("/{resourceType}")
    public ResponseEntity<List<ResourceOption>> listResources(
            @PathVariable String appKey,
            @PathVariable String resourceType,
            @RequestParam(required = false) String connectionId,
            @RequestParam Map<String, String> allParams,
            Authentication auth) {

        UUID userId = null;
        if (auth != null && auth.getPrincipal() instanceof com.crescendo.security.AppUserDetails details) {
            userId = details.getId();
        }

        // Remove known params, leaving only parent cascade params
        Map<String, String> cascadeParams = new HashMap<>(allParams);
        cascadeParams.remove("connectionId");

        // Support "ADMIN_KEY" sentinel for platform-key apps (no user connection)
        UUID connectionUuid = null;
        boolean useAdminKey = (connectionId == null || "ADMIN_KEY".equalsIgnoreCase(connectionId));
        if (!useAdminKey) {
            try {
                connectionUuid = UUID.fromString(connectionId);
            } catch (IllegalArgumentException e) {
                useAdminKey = true; // Treat any non-UUID value as admin-key mode
            }
        }

        List<ResourceOption> options = useAdminKey
                ? resourceFetchService.fetchResourcesWithAdminKey(appKey, resourceType, userId, cascadeParams)
                : (userId != null && connectionUuid != null
                    ? resourceFetchService.fetchResources(appKey, resourceType, connectionUuid, userId, cascadeParams)
                    : resourceFetchService.fetchResourcesWithAdminKey(appKey, resourceType, userId, cascadeParams));

        return ResponseEntity.ok(options);
    }
}
