package com.crescendo.apps.googledrive;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Google Drive shared drive operations.
 *
 * <p>Operations (mirrors n8n {@code Drive.resource.ts}):
 * <ul>
 *   <li>{@code createDrive} — drives.create</li>
 *   <li>{@code deleteDrive} — drives.delete</li>
 *   <li>{@code getDrive}    — drives.get</li>
 *   <li>{@code listDrives}  — drives.list</li>
 *   <li>{@code updateDrive} — drives.update</li>
 * </ul>
 *
 * <p>Credentials: {@code accessToken} (OAuth2, drive scopes)
 */
@Component
public class GoogleDriveDriveHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveDriveHandlers.class);

    private static final String BASE = "https://www.googleapis.com/drive/v3/drives";

    private final RestClient restClient;

    public GoogleDriveDriveHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── createDrive ───────────────────────────────────────────────────────────

    /**
     * Create a new shared drive.
     * Config: name (required)
     */
    @ActionMapping(appKey = "googledrive", actionKey = "createDrive")
    @SuppressWarnings("unchecked")
    public ActionResult createDrive(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String name = require(config, "name");
        if (name == null) return ActionResult.failure("'name' is required");

        logger.info("[googledrive] createDrive: name='{}'", name);

        try {
            // requestId is required by the Drives API to prevent duplicate creations
            String requestId = java.util.UUID.randomUUID().toString();
            Map<String, Object> response = restClient.post()
                    .uri(BASE + "?requestId=" + requestId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("name", name))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledrive] createDrive failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive createDrive failed: " + e.getMessage());
        }
    }

    // ── deleteDrive ───────────────────────────────────────────────────────────

    /**
     * Delete a shared drive (must be empty).
     * Config: driveId (required)
     */
    @ActionMapping(appKey = "googledrive", actionKey = "deleteDrive")
    public ActionResult deleteDrive(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String driveId = require(config, "driveId");
        if (driveId == null) return ActionResult.failure("'driveId' is required");

        logger.info("[googledrive] deleteDrive: driveId='{}'", driveId);

        try {
            restClient.delete()
                    .uri(BASE + "/" + driveId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            return ActionResult.success(Map.of("deleted", true, "id", driveId));
        } catch (Exception e) {
            logger.error("[googledrive] deleteDrive failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive deleteDrive failed: " + e.getMessage());
        }
    }

    // ── getDrive ──────────────────────────────────────────────────────────────

    /**
     * Get a shared drive by ID.
     * Config: driveId (required)
     */
    @ActionMapping(appKey = "googledrive", actionKey = "getDrive")
    @SuppressWarnings("unchecked")
    public ActionResult getDrive(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String driveId = require(config, "driveId");
        if (driveId == null) return ActionResult.failure("'driveId' is required");

        logger.info("[googledrive] getDrive: driveId='{}'", driveId);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE + "/" + driveId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledrive] getDrive failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive getDrive failed: " + e.getMessage());
        }
    }

    // ── listDrives ────────────────────────────────────────────────────────────

    /**
     * List all shared drives the user has access to.
     * Config: pageSize (int, default 20)
     */
    @ActionMapping(appKey = "googledrive", actionKey = "listDrives")
    @SuppressWarnings("unchecked")
    public ActionResult listDrives(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        int pageSize = parseIntOpt(config, "pageSize", 20);

        logger.info("[googledrive] listDrives: pageSize={}", pageSize);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE + "?pageSize=" + pageSize)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledrive] listDrives failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive listDrives failed: " + e.getMessage());
        }
    }

    // ── updateDrive ───────────────────────────────────────────────────────────

    /**
     * Update a shared drive's properties.
     * Config: driveId (required), name (optional)
     */
    @ActionMapping(appKey = "googledrive", actionKey = "updateDrive")
    @SuppressWarnings("unchecked")
    public ActionResult updateDrive(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String driveId = require(config, "driveId");
        if (driveId == null) return ActionResult.failure("'driveId' is required");

        logger.info("[googledrive] updateDrive: driveId='{}'", driveId);

        try {
            Map<String, Object> patch = new HashMap<>();
            if (config.containsKey("name")) patch.put("name", config.get("name"));

            Map<String, Object> response = restClient.patch()
                    .uri(BASE + "/" + driveId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledrive] updateDrive failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive updateDrive failed: " + e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String resolveToken(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        return token != null && !token.toString().isBlank() ? token.toString() : null;
    }

    private ActionResult missingToken() {
        return ActionResult.failure("Google Drive requires an OAuth2 accessToken in connection credentials");
    }

    private String require(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : null;
    }

    private int parseIntOpt(Map<String, Object> config, String key, int defaultVal) {
        Object v = config.get(key);
        if (v == null) return defaultVal;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return defaultVal; }
    }
}
