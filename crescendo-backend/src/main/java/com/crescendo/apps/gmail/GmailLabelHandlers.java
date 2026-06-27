package com.crescendo.apps.gmail;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Gmail label operations.
 *
 * <p>Operations (mirrors n8n v2 {@code LabelDescription.ts}):
 * <ul>
 *   <li>{@code createLabel} — users.labels.create (POST /labels)</li>
 *   <li>{@code deleteLabel} — users.labels.delete (DELETE /labels/{id})</li>
 *   <li>{@code getLabel}    — users.labels.get    (GET  /labels/{id})</li>
 *   <li>{@code getAllLabels}— users.labels.list   (GET  /labels)</li>
 * </ul>
 *
 * <p>Note: Adding/removing labels FROM messages is in {@link GmailMessageHandlers}
 * (addLabels / removeLabels actions), following n8n's resource split where
 * label resource = manage label entities; message resource = manage message labels.
 *
 * <p>Credentials: {@code accessToken} (OAuth2, gmail.labels scope or higher)
 */
@Component
public class GmailLabelHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GmailLabelHandlers.class);

    private static final String BASE = "https://gmail.googleapis.com/gmail/v1/users/me/labels";

    private final RestClient restClient;

    public GmailLabelHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    /**
     * Create a Gmail label.
     * Config: name (required),
     *         labelListVisibility (labelHide|labelShow|labelShowIfUnread, default: labelShow),
     *         messageListVisibility (hide|show, default: show)
     */
    @ActionMapping(appKey = "gmail", actionKey = "createLabel")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String name = require(config, "name");
        if (name == null) return ActionResult.failure("'name' is required");

        String labelListVisibility = opt(config, "labelListVisibility", "labelShow");
        String messageListVisibility = opt(config, "messageListVisibility", "show");

        logger.info("[gmail] createLabel: name='{}'", name);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("labelListVisibility", labelListVisibility);
            body.put("messageListVisibility", messageListVisibility);

            Map<String, Object> response = restClient.post()
                    .uri(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] createLabel failed: {}", e.getMessage());
            return ActionResult.failure("Gmail createLabel failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    /**
     * Delete a Gmail label by ID.
     * Config: labelId (required)
     */
    @ActionMapping(appKey = "gmail", actionKey = "deleteLabel")
    public ActionResult delete(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String labelId = require(config, "labelId");
        if (labelId == null) return ActionResult.failure("'labelId' is required");

        logger.info("[gmail] deleteLabel: labelId='{}'", labelId);

        try {
            restClient.delete()
                    .uri(BASE + "/" + labelId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            return ActionResult.success(Map.of("deleted", true, "id", labelId));
        } catch (Exception e) {
            logger.error("[gmail] deleteLabel failed: {}", e.getMessage());
            return ActionResult.failure("Gmail deleteLabel failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────

    /**
     * Get a Gmail label by ID.
     * Config: labelId (required)
     */
    @ActionMapping(appKey = "gmail", actionKey = "getLabel")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String labelId = require(config, "labelId");
        if (labelId == null) return ActionResult.failure("'labelId' is required");

        logger.info("[gmail] getLabel: labelId='{}'", labelId);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE + "/" + labelId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] getLabel failed: {}", e.getMessage());
            return ActionResult.failure("Gmail getLabel failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    /**
     * List all Gmail labels for the authenticated user.
     * Config: none required (the Gmail labels.list API returns all labels)
     */
    @ActionMapping(appKey = "gmail", actionKey = "getAllLabels")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        logger.info("[gmail] getAllLabels");

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] getAllLabels failed: {}", e.getMessage());
            return ActionResult.failure("Gmail getAllLabels failed: " + e.getMessage());
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
        return ActionResult.failure("Gmail requires an OAuth2 accessToken in connection credentials");
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
