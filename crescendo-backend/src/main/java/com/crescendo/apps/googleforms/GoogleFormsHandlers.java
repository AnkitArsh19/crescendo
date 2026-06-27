package com.crescendo.apps.googleforms;

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
 * Grouped handler for Google Forms operations (Crescendo-only — no n8n equivalent).
 *
 * <p>Operations:
 * <ul>
 *   <li>{@code createForm}    — forms.create    (POST /v1/forms)</li>
 *   <li>{@code getForm}       — forms.get        (GET  /v1/forms/{id})</li>
 *   <li>{@code listResponses} — forms.responses.list (GET /v1/forms/{id}/responses)</li>
 * </ul>
 *
 * <p>Credentials: {@code accessToken} (OAuth2, forms scope)
 */
@Component
public class GoogleFormsHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GoogleFormsHandlers.class);

    private static final String BASE = "https://forms.googleapis.com/v1/forms";

    private final RestClient restClient;

    public GoogleFormsHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── createForm ────────────────────────────────────────────────────────────

    /**
     * Create a new Google Form.
     * Config: title (required), documentTitle (optional)
     */
    @ActionMapping(appKey = "googleforms", actionKey = "createForm")
    @SuppressWarnings("unchecked")
    public ActionResult createForm(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String title = require(config, "title");
        if (title == null) return ActionResult.failure("'title' is required");

        logger.info("[googleforms] createForm: title='{}'", title);

        try {
            Map<String, Object> info = new HashMap<>();
            info.put("title", title);
            String docTitle = opt(config, "documentTitle", null);
            if (docTitle != null) info.put("documentTitle", docTitle);

            Map<String, Object> response = restClient.post()
                    .uri(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("info", info))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googleforms] createForm failed: {}", e.getMessage());
            return ActionResult.failure("Google Forms createForm failed: " + e.getMessage());
        }
    }

    // ── getForm ───────────────────────────────────────────────────────────────

    /**
     * Get a Google Form by form ID.
     * Config: formId (required)
     */
    @ActionMapping(appKey = "googleforms", actionKey = "getForm")
    @SuppressWarnings("unchecked")
    public ActionResult getForm(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String formId = require(config, "formId");
        if (formId == null) return ActionResult.failure("'formId' is required");

        logger.info("[googleforms] getForm: formId='{}'", formId);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE + "/" + formId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googleforms] getForm failed: {}", e.getMessage());
            return ActionResult.failure("Google Forms getForm failed: " + e.getMessage());
        }
    }

    // ── listResponses ─────────────────────────────────────────────────────────

    /**
     * List responses for a Google Form.
     * Config: formId (required), pageSize (int, default 50)
     */
    @ActionMapping(appKey = "googleforms", actionKey = "listResponses")
    @SuppressWarnings("unchecked")
    public ActionResult listResponses(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String formId = require(config, "formId");
        if (formId == null) return ActionResult.failure("'formId' is required");
        int pageSize = parseIntOpt(config, "pageSize", 50);

        logger.info("[googleforms] listResponses: formId='{}', pageSize={}", formId, pageSize);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE + "/" + formId + "/responses?pageSize=" + pageSize)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googleforms] listResponses failed: {}", e.getMessage());
            return ActionResult.failure("Google Forms listResponses failed: " + e.getMessage());
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
        return ActionResult.failure("Google Forms requires an OAuth2 accessToken in connection credentials");
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
}
