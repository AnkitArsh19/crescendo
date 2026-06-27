package com.crescendo.apps.gmail;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Grouped handler for Gmail draft operations.
 *
 * <p>Operations (mirrors n8n v2 {@code DraftDescription.ts}):
 * <ul>
 *   <li>{@code create}  — users.drafts.create (POST /drafts)</li>
 *   <li>{@code delete}  — users.drafts.delete (DELETE /drafts/{id})</li>
 *   <li>{@code get}     — users.drafts.get    (GET  /drafts/{id})</li>
 *   <li>{@code getAll}  — users.drafts.list   (GET  /drafts)</li>
 * </ul>
 *
 * <p>Credentials: {@code accessToken} (OAuth2, gmail scopes)
 */
@Component
public class GmailDraftHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GmailDraftHandlers.class);

    private static final String BASE = "https://gmail.googleapis.com/gmail/v1/users/me/drafts";

    private final RestClient restClient;

    public GmailDraftHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    /**
     * Create a Gmail draft.
     * Config: subject (required), message (required), emailType (html|text),
     *         to (optional — sendTo in n8n), cc (ccList), bcc (bccList),
     *         replyTo, threadId
     */
    @ActionMapping(appKey = "gmail", actionKey = "createDraft")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String subject = require(config, "subject");
        if (subject == null) return ActionResult.failure("'subject' is required");
        String message = require(config, "message");
        if (message == null) return ActionResult.failure("'message' is required");

        String emailType = opt(config, "emailType", "text");
        String to = opt(config, "to", null);
        String cc = opt(config, "ccList", null);
        String bcc = opt(config, "bccList", null);
        String replyTo = opt(config, "replyTo", null);
        String threadId = opt(config, "threadId", null);

        logger.info("[gmail] createDraft: subject='{}'", subject);

        try {
            List<String> lines = new ArrayList<>();
            if (to != null) lines.add("To: " + to);
            if (cc != null) lines.add("Cc: " + cc);
            if (bcc != null) lines.add("Bcc: " + bcc);
            if (replyTo != null) lines.add("Reply-To: " + replyTo);
            lines.add("Subject: " + subject);
            String contentType = "html".equals(emailType) ? "text/html" : "text/plain";
            lines.add("Content-Type: " + contentType + "; charset=UTF-8");
            lines.add("");
            lines.add(message);

            String raw = String.join("\r\n", lines);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> msgPayload = threadId != null
                    ? Map.of("raw", encoded, "threadId", threadId)
                    : Map.of("raw", encoded);

            Map<String, Object> body = Map.of("message", msgPayload);

            Map<String, Object> response = restClient.post()
                    .uri(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] createDraft failed: {}", e.getMessage());
            return ActionResult.failure("Gmail createDraft failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    /**
     * Delete a Gmail draft by ID.
     * Config: messageId (required — Draft ID)
     */
    @ActionMapping(appKey = "gmail", actionKey = "deleteDraft")
    public ActionResult delete(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String draftId = require(config, "messageId");
        if (draftId == null) return ActionResult.failure("'messageId' (draft ID) is required");

        logger.info("[gmail] deleteDraft: draftId='{}'", draftId);

        try {
            restClient.delete()
                    .uri(BASE + "/" + draftId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            return ActionResult.success(Map.of("deleted", true, "id", draftId));
        } catch (Exception e) {
            logger.error("[gmail] deleteDraft failed: {}", e.getMessage());
            return ActionResult.failure("Gmail deleteDraft failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────

    /**
     * Get a Gmail draft by ID.
     * Config: messageId (required — Draft ID)
     */
    @ActionMapping(appKey = "gmail", actionKey = "getDraft")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String draftId = require(config, "messageId");
        if (draftId == null) return ActionResult.failure("'messageId' (draft ID) is required");

        logger.info("[gmail] getDraft: draftId='{}'", draftId);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE + "/" + draftId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] getDraft failed: {}", e.getMessage());
            return ActionResult.failure("Gmail getDraft failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    /**
     * List Gmail drafts.
     * Config: returnAll (bool), limit (int, default 50),
     *         includeSpamTrash (bool)
     */
    @ActionMapping(appKey = "gmail", actionKey = "getAllDrafts")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        boolean returnAll = Boolean.parseBoolean(opt(config, "returnAll", "false"));
        int limit = parseIntOpt(config, "limit", 50);
        boolean includeSpamTrash = Boolean.parseBoolean(opt(config, "includeSpamTrash", "false"));

        logger.info("[gmail] getAllDrafts: limit={}, returnAll={}", limit, returnAll);

        try {
            StringBuilder url = new StringBuilder(BASE);
            url.append("?maxResults=").append(returnAll ? 500 : limit);
            if (includeSpamTrash) url.append("&includeSpamTrash=true");

            Map<String, Object> response = restClient.get()
                    .uri(url.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] getAllDrafts failed: {}", e.getMessage());
            return ActionResult.failure("Gmail getAllDrafts failed: " + e.getMessage());
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

    private int parseIntOpt(Map<String, Object> config, String key, int defaultVal) {
        Object v = config.get(key);
        if (v == null) return defaultVal;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return defaultVal; }
    }
}
