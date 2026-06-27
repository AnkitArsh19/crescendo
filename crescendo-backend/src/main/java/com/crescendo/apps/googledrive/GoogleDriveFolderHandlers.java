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
import java.util.List;
import java.util.Map;

/**
 * Grouped handler for Google Drive folder operations.
 *
 * <p>Operations (mirrors n8n {@code Folder.resource.ts}):
 * <ul>
 *   <li>{@code createFolder} — files.create with folder mimeType</li>
 *   <li>{@code deleteFolder} — files.delete</li>
 *   <li>{@code shareFolder}  — permissions.create on folder</li>
 * </ul>
 *
 * <p>Credentials: {@code accessToken} (OAuth2, drive scopes)
 */
@Component
public class GoogleDriveFolderHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveFolderHandlers.class);

    private static final String BASE = "https://www.googleapis.com/drive/v3/files";
    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";

    private final RestClient restClient;

    public GoogleDriveFolderHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── createFolder ──────────────────────────────────────────────────────────

    /**
     * Create a new folder in Google Drive.
     * Config: name (required), folderId (optional — parent folder ID),
     *         driveId (optional — shared drive ID)
     */
    @ActionMapping(appKey = "googledrive", actionKey = "createFolder")
    @SuppressWarnings("unchecked")
    public ActionResult createFolder(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String name = require(config, "name");
        if (name == null) return ActionResult.failure("'name' is required");
        String parentId = opt(config, "folderId", null);
        String driveId = opt(config, "driveId", null);

        logger.info("[googledrive] createFolder: name='{}'", name);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("mimeType", FOLDER_MIME);
            if (parentId != null) body.put("parents", List.of(parentId));
            else if (driveId != null) body.put("parents", List.of(driveId));

            Map<String, Object> response = restClient.post()
                    .uri(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledrive] createFolder failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive createFolder failed: " + e.getMessage());
        }
    }

    // ── deleteFolder ──────────────────────────────────────────────────────────

    /**
     * Permanently delete a folder (and all its contents).
     * Config: folderId (required)
     */
    @ActionMapping(appKey = "googledrive", actionKey = "deleteFolder")
    public ActionResult deleteFolder(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String folderId = require(config, "folderId");
        if (folderId == null) return ActionResult.failure("'folderId' is required");

        logger.info("[googledrive] deleteFolder: folderId='{}'", folderId);

        try {
            restClient.delete()
                    .uri(BASE + "/" + folderId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            return ActionResult.success(Map.of("deleted", true, "id", folderId));
        } catch (Exception e) {
            logger.error("[googledrive] deleteFolder failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive deleteFolder failed: " + e.getMessage());
        }
    }

    // ── shareFolder ───────────────────────────────────────────────────────────

    /**
     * Share a folder with a user or group.
     * Config: folderId (required), email (required), role (reader|writer|commenter, default "reader"),
     *         type (user|group|domain|anyone, default "user")
     */
    @ActionMapping(appKey = "googledrive", actionKey = "shareFolder")
    @SuppressWarnings("unchecked")
    public ActionResult shareFolder(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String folderId = require(config, "folderId");
        if (folderId == null) return ActionResult.failure("'folderId' is required");
        String email = opt(config, "email", null);
        String role = opt(config, "role", "reader");
        String type = opt(config, "type", "user");

        logger.info("[googledrive] shareFolder: folderId='{}', email='{}', role='{}'", folderId, email, role);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("role", role);
            body.put("type", type);
            if (email != null) body.put("emailAddress", email);

            Map<String, Object> response = restClient.post()
                    .uri("https://www.googleapis.com/drive/v3/files/" + folderId + "/permissions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledrive] shareFolder failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive shareFolder failed: " + e.getMessage());
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

    private String opt(Map<String, Object> config, String key, String defaultVal) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : defaultVal;
    }
}
