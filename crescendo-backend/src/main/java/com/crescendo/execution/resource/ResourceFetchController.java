package com.crescendo.execution.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            @RequestParam UUID connectionId,
            @RequestParam Map<String, String> allParams,
            Authentication auth) {

        UUID userId = resourceFetchService.extractUserId(auth);

        // Remove known params, leaving only parent cascade params
        Map<String, String> cascadeParams = new HashMap<>(allParams);
        cascadeParams.remove("connectionId");

        List<ResourceOption> options = resourceFetchService.fetchResources(
                appKey, resourceType, connectionId, userId, cascadeParams);

        return ResponseEntity.ok(options);
    }
}
