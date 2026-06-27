package com.crescendo.apps.googledocs;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Grouped handler for Google Docs operations.
 *
 * <p>Operations (mirrors n8n {@code DocumentDescription.ts}):
 * <ul>
 *   <li>{@code create}      — documents.create (POST /documents)</li>
 *   <li>{@code get}         — documents.get    (GET  /documents/{id})</li>
 *   <li>{@code update}      — documents.batchUpdate with {@code appendText} (insertText at end)</li>
 *   <li>{@code replaceText} — documents.batchUpdate with {@code replaceAllText}</li>
 * </ul>
 *
 * <p>Crescendo maps n8n's "update" to appendText and adds replaceText as its own op.
 * <p>Credentials: {@code accessToken} (OAuth2, drive + docs scopes)
 */
@Component
public class GoogleDocsHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDocsHandlers.class);

    private static final String DOCS_BASE = "https://docs.googleapis.com/v1/documents";

    private final RestClient restClient;

    public GoogleDocsHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    /**
     * Create a new Google Doc.
     * Config: title (required), folderId (optional — moves doc into that Drive folder after creation)
     */
    @ActionMapping(appKey = "googledocs", actionKey = "create")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String title = require(config, "title");
        if (title == null) return ActionResult.failure("'title' is required");
        String folderId = opt(config, "folderId", null);

        logger.info("[googledocs] create: title='{}'", title);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(DOCS_BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("title", title))
                    .retrieve()
                    .body(Map.class);

            // If folderId provided, move the doc into that folder via Drive API
            String documentId = response != null ? (String) response.get("documentId") : null;
            if (folderId != null && documentId != null) {
                logger.info("[googledocs] Moving doc '{}' to folder '{}'", documentId, folderId);
                RestClient.create().patch()
                        .uri("https://www.googleapis.com/drive/v3/files/" + documentId + "?addParents=" + folderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .toBodilessEntity();
            }

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledocs] create failed: {}", e.getMessage());
            return ActionResult.failure("Google Docs create failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────

    /**
     * Get a Google Doc by document ID.
     * Config: documentId (required)
     */
    @ActionMapping(appKey = "googledocs", actionKey = "get")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String documentId = require(config, "documentId");
        if (documentId == null) return ActionResult.failure("'documentId' is required");

        logger.info("[googledocs] get: documentId='{}'", documentId);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(DOCS_BASE + "/" + documentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledocs] get failed: {}", e.getMessage());
            return ActionResult.failure("Google Docs get failed: " + e.getMessage());
        }
    }

    // ── appendText ────────────────────────────────────────────────────────────

    /**
     * Append text to the end of a Google Doc (n8n "update" operation).
     * Config: documentId (required), text (required)
     */
    @ActionMapping(appKey = "googledocs", actionKey = "appendText")
    @SuppressWarnings("unchecked")
    public ActionResult appendText(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String documentId = require(config, "documentId");
        if (documentId == null) return ActionResult.failure("'documentId' is required");
        String text = require(config, "text");
        if (text == null) return ActionResult.failure("'text' is required");

        logger.info("[googledocs] appendText: documentId='{}'", documentId);

        try {
            Map<String, Object> body = Map.of(
                    "requests", List.of(Map.of(
                            "insertText", Map.of(
                                    "endOfSegmentLocation", Map.of(),
                                    "text", text
                            )
                    ))
            );

            Map<String, Object> response = restClient.post()
                    .uri(DOCS_BASE + "/" + documentId + ":batchUpdate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googledocs] appendText failed: {}", e.getMessage());
            return ActionResult.failure("Google Docs appendText failed: " + e.getMessage());
        }
    }

    // ── replaceText ───────────────────────────────────────────────────────────

    /**
     * Replace all occurrences of text in a Google Doc.
     * Config: documentId (required), searchText (required), replaceText (required),
     *         matchCase (bool, default false)
     */
    @ActionMapping(appKey = "googledocs", actionKey = "replaceText")
    @SuppressWarnings("unchecked")
    public ActionResult replaceText(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String documentId = require(config, "documentId");
        if (documentId == null) return ActionResult.failure("'documentId' is required");
        String searchText = require(config, "searchText");
        if (searchText == null) return ActionResult.failure("'searchText' is required");
        String replaceText = require(config, "replaceText");
        if (replaceText == null) return ActionResult.failure("'replaceText' is required");
        boolean matchCase = Boolean.parseBoolean(opt(config, "matchCase", "false"));

        logger.info("[googledocs] replaceText: documentId='{}', search='{}'", documentId, searchText);

        try {
            Map<String, Object> body = Map.of(
                    "requests", List.of(Map.of(
                            "replaceAllText", Map.of(
                                    "containsText", Map.of("text", searchText, "matchCase", matchCase),
                                    "replaceText", replaceText
                            )
                    ))
            );

            Map<String, Object> response = restClient.post()
                    .uri(DOCS_BASE + "/" + documentId + ":batchUpdate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            // Extract occurrencesChanged from replies[0].replaceAllText
            int occurrences = 0;
            if (response != null && response.containsKey("replies")) {
                var replies = (List<Map<String, Object>>) response.get("replies");
                if (!replies.isEmpty()) {
                    var replaceResult = (Map<String, Object>) replies.get(0).get("replaceAllText");
                    if (replaceResult != null && replaceResult.containsKey("occurrencesChanged")) {
                        occurrences = ((Number) replaceResult.get("occurrencesChanged")).intValue();
                    }
                }
            }

            return ActionResult.success(Map.of(
                    "documentId", documentId,
                    "occurrencesChanged", occurrences
            ));
        } catch (Exception e) {
            logger.error("[googledocs] replaceText failed: {}", e.getMessage());
            return ActionResult.failure("Google Docs replaceText failed: " + e.getMessage());
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
        return ActionResult.failure("Google Docs requires an OAuth2 accessToken in connection credentials");
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
