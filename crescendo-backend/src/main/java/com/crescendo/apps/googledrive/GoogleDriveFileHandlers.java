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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grouped handler for Google Drive file operations.
 *
 * <p>Operations (mirrors n8n {@code File.resource.ts}):
 * <ul>
 *   <li>{@code copy}    — files.copy</li>
 *   <li>{@code delete}  — files.delete</li>
 *   <li>{@code download}— files.get with alt=media</li>
 *   <li>{@code getAll}  — files.list</li>
 *   <li>{@code move}    — files.update (addParents/removeParents)</li>
 *   <li>{@code share}   — permissions.create</li>
 *   <li>{@code upload}  — files.create (metadata-only; binary via Drive multipart)</li>
 * </ul>
 *
 * <p>Credentials: {@code accessToken} (OAuth2, drive scopes)
 */
@Component
public class GoogleDriveFileHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveFileHandlers.class);

    private static final String BASE = "https://www.googleapis.com/drive/v3/files";

    private final RestClient restClient;
    private final com.crescendo.storage.MediaStreamResolver mediaStreamResolver;

    public GoogleDriveFileHandlers() {
        this(new com.crescendo.storage.MediaStreamResolver());
    }

    public GoogleDriveFileHandlers(@org.springframework.beans.factory.annotation.Autowired com.crescendo.storage.MediaStreamResolver mediaStreamResolver) {
        this.mediaStreamResolver = mediaStreamResolver;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── copy ──────────────────────────────────────────────────────────────────

    /**
     * Copy a file in Google Drive.
     * Config: fileId (required), name (optional — new name for copy),
     *         folderId (optional — destination folder)
     */
    @ActionMapping(appKey = "google-drive", actionKey = "copy")
    @SuppressWarnings("unchecked")
    public ActionResult copy(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String fileId = require(config, "fileId");
        if (fileId == null) return ActionResult.failure("'fileId' is required");
        String name = opt(config, "name", null);
        String folderId = opt(config, "folderId", null);

        logger.info("[googledrive] copy: fileId='{}', newName='{}'", fileId, name);

        try {
            Map<String, Object> body = new HashMap<>();
            if (name != null) body.put("name", name);
            if (folderId != null) body.put("parents", List.of(folderId));

            Map<String, Object> response = restClient.post()
                    .uri(BASE + "/" + fileId + "/copy")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledrive] copy failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive copy failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    /**
     * Permanently delete a file.
     * Config: fileId (required)
     */
    @ActionMapping(appKey = "google-drive", actionKey = "deleteFile")
    public ActionResult delete(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String fileId = require(config, "fileId");
        if (fileId == null) return ActionResult.failure("'fileId' is required");

        logger.info("[googledrive] deleteFile: fileId='{}'", fileId);

        try {
            restClient.delete()
                    .uri(BASE + "/" + fileId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            return ActionResult.success(Map.of("deleted", true, "id", fileId));
        } catch (Exception e) {
            logger.error("[googledrive] deleteFile failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive deleteFile failed: " + e.getMessage());
        }
    }

    // ── download ──────────────────────────────────────────────────────────────

    /**
     * Download a file (returns raw bytes as base64 string).
     * Config: fileId (required)
     */
    @ActionMapping(appKey = "google-drive", actionKey = "download")
    public ActionResult download(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String fileId = require(config, "fileId");
        if (fileId == null) return ActionResult.failure("'fileId' is required");

        logger.info("[googledrive] download: fileId='{}'", fileId);

        try {
            byte[] bytes = restClient.get()
                    .uri(BASE + "/" + fileId + "?alt=media")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(byte[].class);

            return ActionResult.success(Map.of(
                    "fileId", fileId,
                    "data", java.util.Base64.getEncoder().encodeToString(bytes != null ? bytes : new byte[0])
            ));
        } catch (Exception e) {
            logger.error("[googledrive] download failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive download failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    /**
     * List files in Google Drive.
     * Config: query (Drive query string), folderId (filters to folder),
     *         pageSize (int, default 20), fields (comma-sep Drive fields)
     */
    @ActionMapping(appKey = "google-drive", actionKey = "getAll")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        int pageSize = parseIntOpt(config, "pageSize", 20);
        String query = opt(config, "query", "");
        String folderId = opt(config, "folderId", null);
        String fields = opt(config, "fields", "files(id,name,mimeType,modifiedTime,size)");

        if (folderId != null) {
            String parentQ = "'" + folderId + "' in parents";
            query = query.isBlank() ? parentQ : query + " and " + parentQ;
        }

        logger.info("[googledrive] getAll: pageSize={}, query='{}'", pageSize, query);

        try {
            StringBuilder url = new StringBuilder(BASE + "?pageSize=" + pageSize + "&fields=" + encode(fields));
            if (!query.isBlank()) url.append("&q=").append(encode(query));

            Map<String, Object> response = restClient.get()
                    .uri(url.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledrive] getAll failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive getAll failed: " + e.getMessage());
        }
    }

    // ── move ──────────────────────────────────────────────────────────────────

    /**
     * Move a file to a different folder.
     * Config: fileId (required), folderId (required — destination)
     */
    @ActionMapping(appKey = "google-drive", actionKey = "move")
    @SuppressWarnings("unchecked")
    public ActionResult move(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String fileId = require(config, "fileId");
        if (fileId == null) return ActionResult.failure("'fileId' is required");
        String folderId = require(config, "folderId");
        if (folderId == null) return ActionResult.failure("'folderId' is required");

        logger.info("[googledrive] move: fileId='{}', folderId='{}'", fileId, folderId);

        try {
            // Fetch current parents to remove them
            Map<String, Object> meta = restClient.get()
                    .uri(BASE + "/" + fileId + "?fields=parents")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            List<String> prevParents = meta != null ? (List<String>) meta.get("parents") : List.of();
            String removeParents = String.join(",", prevParents);

            String url = BASE + "/" + fileId + "?addParents=" + encode(folderId)
                    + (removeParents.isBlank() ? "" : "&removeParents=" + encode(removeParents))
                    + "&fields=id,parents";

            Map<String, Object> response = restClient.patch()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of())
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledrive] move failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive move failed: " + e.getMessage());
        }
    }

    // ── share ─────────────────────────────────────────────────────────────────

    /**
     * Share a file with a user or group.
     * Config: fileId (required), email (required), role (reader|writer|commenter, default "reader"),
     *         type (user|group|domain|anyone, default "user")
     */
    @ActionMapping(appKey = "google-drive", actionKey = "share")
    @SuppressWarnings("unchecked")
    public ActionResult share(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String fileId = require(config, "fileId");
        if (fileId == null) return ActionResult.failure("'fileId' is required");
        String email = opt(config, "email", null);
        String role = opt(config, "role", "reader");
        String type = opt(config, "type", "user");

        logger.info("[googledrive] share: fileId='{}', email='{}', role='{}'", fileId, email, role);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("role", role);
            body.put("type", type);
            if (email != null) body.put("emailAddress", email);

            Map<String, Object> response = restClient.post()
                    .uri("https://www.googleapis.com/drive/v3/files/" + fileId + "/permissions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledrive] share failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive share failed: " + e.getMessage());
        }
    }

    // ── upload ────────────────────────────────────────────────────────────────

    /**
     * Upload a file (metadata-only create; content via separate Drive upload API).
     * Config: name (required), mimeType (required), folderId (optional)
     */
    @ActionMapping(appKey = "google-drive", actionKey = "upload")
    @SuppressWarnings("unchecked")
    public ActionResult upload(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String name = opt(config, "fileName", null);
        if (name == null) name = opt(config, "name", null);

        String mimeType = opt(config, "mimeType", null);
        String folderId = opt(config, "folderId", null);
        Object rawFile = config.get("file");
        if (rawFile == null) rawFile = config.get("content");
        if (rawFile == null) rawFile = config.get("fileUrl");

        try {
            if (rawFile != null && mediaStreamResolver != null) {
                try (com.crescendo.storage.MediaStreamResolver.MediaSource media = mediaStreamResolver.resolve(rawFile, mimeType != null ? mimeType : "application/octet-stream")) {
                    String resolvedName = (name != null && !name.isBlank()) ? name : (media.filename() != null ? media.filename() : "uploaded_file");
                    String resolvedMime = (mimeType != null && !mimeType.isBlank()) ? mimeType : (media.contentType() != null ? media.contentType() : "application/octet-stream");

                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    media.stream().transferTo(baos);
                    byte[] fileBytes = baos.toByteArray();

                    String boundary = "crescendo-gdrive-" + java.util.UUID.randomUUID();
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("name", resolvedName);
                    metadata.put("mimeType", resolvedMime);
                    if (folderId != null && !folderId.isBlank()) metadata.put("parents", List.of(folderId));

                    tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
                    String metaJson = mapper.writeValueAsString(metadata);

                    byte[] head = ("--" + boundary + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" + metaJson + "\r\n--" + boundary + "\r\nContent-Type: " + resolvedMime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
                    byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

                    byte[] multipartBody = new byte[head.length + fileBytes.length + tail.length];
                    System.arraycopy(head, 0, multipartBody, 0, head.length);
                    System.arraycopy(fileBytes, 0, multipartBody, head.length, fileBytes.length);
                    System.arraycopy(tail, 0, multipartBody, head.length + fileBytes.length, tail.length);

                    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .header("Content-Type", "multipart/related; boundary=" + boundary)
                            .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                            .build();

                    java.net.http.HttpResponse<String> resp = java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        Map<String, Object> result = mapper.readValue(resp.body(), Map.class);
                        return ActionResult.success(result);
                    }
                    return ActionResult.failure("Google Drive upload failed (" + resp.statusCode() + "): " + resp.body());
                }
            }

            if (name == null) return ActionResult.failure("'name' or 'fileName' is required");
            if (mimeType == null) mimeType = "application/octet-stream";

            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("mimeType", mimeType);
            if (folderId != null) body.put("parents", List.of(folderId));

            Map<String, Object> response = restClient.post()
                    .uri(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledrive] upload failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive upload failed: " + e.getMessage());
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

    private int parseIntOpt(Map<String, Object> config, String key, int defaultVal) {
        Object v = config.get(key);
        if (v == null) return defaultVal;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return defaultVal; }
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
