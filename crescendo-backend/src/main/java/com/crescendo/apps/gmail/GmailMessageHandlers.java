package com.crescendo.apps.gmail;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grouped handler for all Gmail message operations.
 *
 * <p>Operations (mirrors n8n v2 {@code MessageDescription.ts}):
 * <ul>
 *   <li>{@code send}         — users.messages.send</li>
 *   <li>{@code reply}        — users.messages.send with threadId + In-Reply-To header</li>
 *   <li>{@code get}          — users.messages.get</li>
 *   <li>{@code getAll}       — users.messages.list (with filters)</li>
 *   <li>{@code delete}       — users.messages.delete</li>
 *   <li>{@code markAsRead}   — users.messages.modify (removeLabelIds: UNREAD)</li>
 *   <li>{@code markAsUnread} — users.messages.modify (addLabelIds: UNREAD)</li>
 *   <li>{@code addLabels}    — users.messages.modify (addLabelIds)</li>
 *   <li>{@code removeLabels} — users.messages.modify (removeLabelIds)</li>
 *   <li>{@code search}       — users.messages.list with q parameter</li>
 * </ul>
 *
 * <p>Credentials: {@code accessToken} (OAuth2, gmail scopes)
 */
@Component
public class GmailMessageHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GmailMessageHandlers.class);

    private static final String BASE = "https://gmail.googleapis.com/gmail/v1/users/me/messages";

    private final RestClient restClient;

    public GmailMessageHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    // ── send ─────────────────────────────────────────────────────────────────

    /**
     * Send a Gmail message.
     * Config: to (required), subject (required), message (required),
     *         emailType (html|text), cc, bcc, replyTo, senderName, threadId
     */
    @ActionMapping(appKey = "gmail", actionKey = "send")
    public ActionResult send(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String to = require(config, "to");
        if (to == null) return ActionResult.failure("'to' is required");
        String subject = require(config, "subject");
        if (subject == null) return ActionResult.failure("'subject' is required");
        String message = require(config, "message");
        if (message == null) return ActionResult.failure("'message' is required");

        String emailType = opt(config, "emailType", "html");
        String cc = opt(config, "cc", null);
        String bcc = opt(config, "bcc", null);
        String replyTo = opt(config, "replyTo", null);
        String senderName = opt(config, "senderName", null);
        String threadId = opt(config, "threadId", null);

        logger.info("[gmail] send: to='{}', subject='{}'", to, subject);

        try {
            List<String> lines = new ArrayList<>();
            String fromHeader = senderName != null ? senderName + " <me>" : null;
            if (fromHeader != null) lines.add("From: " + fromHeader);
            lines.add("To: " + to);
            if (cc != null) lines.add("Cc: " + cc);
            if (bcc != null) lines.add("Bcc: " + bcc);
            if (replyTo != null) lines.add("Reply-To: " + replyTo);
            lines.add("Subject: " + subject);
            String contentType = "html".equals(emailType) ? "text/html" : "text/plain";
            lines.add("Content-Type: " + contentType + "; charset=UTF-8");
            lines.add("");
            lines.add(message);

            Map<String, Object> body = new HashMap<>();
            body.put("raw", encodeRaw(lines));
            if (threadId != null) body.put("threadId", threadId);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(BASE + "/send")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] send failed: {}", e.getMessage());
            return ActionResult.failure("Gmail send failed: " + e.getMessage());
        }
    }

    // ── reply ─────────────────────────────────────────────────────────────────

    /**
     * Reply to an existing Gmail thread.
     * Config: messageId (required), message (required), emailType (html|text),
     *         replyToSenderOnly (bool)
     */
    @ActionMapping(appKey = "gmail", actionKey = "reply")
    @SuppressWarnings("unchecked")
    public ActionResult reply(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String messageId = require(config, "messageId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");
        String message = require(config, "message");
        if (message == null) return ActionResult.failure("'message' is required");

        String emailType = opt(config, "emailType", "html");
// boolean replyToSenderOnly = Boolean.parseBoolean(opt(config, "replyToSenderOnly", "false"));

        logger.info("[gmail] reply: messageId='{}'", messageId);

        try {
            // Fetch original to get thread/subject/headers
            Map<String, Object> original = restClient.get()
                    .uri(BASE + "/" + messageId + "?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=To&metadataHeaders=Message-Id")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            String threadId = (String) original.get("threadId");
            String toAddress = "";
            String subject = "";
            String origMsgId = "";

            var payload = (Map<String, Object>) original.get("payload");
            if (payload != null) {
                var headers = (List<Map<String, String>>) payload.get("headers");
                if (headers != null) {
                    for (var h : headers) {
                        String name = h.get("name");
                        if ("From".equalsIgnoreCase(name)) toAddress = h.get("value");
                        if ("Subject".equalsIgnoreCase(name)) subject = h.get("value");
                        if ("Message-Id".equalsIgnoreCase(name)) origMsgId = h.get("value");
                    }
                }
            }

            if (!subject.toLowerCase().startsWith("re:")) subject = "Re: " + subject;

            List<String> lines = new ArrayList<>();
            lines.add("To: " + toAddress);
            lines.add("Subject: " + subject);
            String contentType = "html".equals(emailType) ? "text/html" : "text/plain";
            lines.add("Content-Type: " + contentType + "; charset=UTF-8");
            if (!origMsgId.isEmpty()) {
                lines.add("In-Reply-To: " + origMsgId);
                lines.add("References: " + origMsgId);
            }
            lines.add("");
            lines.add(message);

            Map<String, Object> body = new HashMap<>();
            body.put("raw", encodeRaw(lines));
            if (threadId != null) body.put("threadId", threadId);

            Map<String, Object> response = restClient.post()
                    .uri(BASE + "/send")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] reply failed: {}", e.getMessage());
            return ActionResult.failure("Gmail reply failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────

    /**
     * Get a single Gmail message by ID.
     * Config: messageId (required)
     */
    @ActionMapping(appKey = "gmail", actionKey = "get")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String messageId = require(config, "messageId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");

        logger.info("[gmail] get: messageId='{}'", messageId);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE + "/" + messageId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] get failed: {}", e.getMessage());
            return ActionResult.failure("Gmail get failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    /**
     * List Gmail messages with optional filters.
     * Config: returnAll (bool), limit (int, default 50),
     *         filters.q (search query), filters.labelIds (comma-sep),
     *         filters.includeSpamTrash (bool), filters.readStatus (unread|read|both),
     *         filters.sender, filters.receivedAfter, filters.receivedBefore
     */
    @ActionMapping(appKey = "gmail", actionKey = "getAll")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        boolean returnAll = Boolean.parseBoolean(opt(config, "returnAll", "false"));
        int limit = parseIntOpt(config, "limit", 50);

        // Filters (mirrors n8n getAll filter collection)
        String q = opt(config, "q", "");
        String labelIds = opt(config, "labelIds", null);
        boolean includeSpamTrash = Boolean.parseBoolean(opt(config, "includeSpamTrash", "false"));
        String readStatus = opt(config, "readStatus", "both");
        String sender = opt(config, "sender", null);

        // Build Gmail query string
        StringBuilder query = new StringBuilder(q != null ? q : "");
        if ("unread".equals(readStatus)) appendQ(query, "is:unread");
        else if ("read".equals(readStatus)) appendQ(query, "is:read");
        if (sender != null && !sender.isBlank()) appendQ(query, "from:" + sender);

        logger.info("[gmail] getAll: query='{}', limit={}, returnAll={}", query, limit, returnAll);

        try {
            StringBuilder url = new StringBuilder(BASE);
            url.append("?maxResults=").append(returnAll ? 500 : limit);
            if (!query.isEmpty()) url.append("&q=").append(URLEncoder.encode(query.toString(), StandardCharsets.UTF_8));
            if (labelIds != null && !labelIds.isBlank()) url.append("&labelIds=").append(URLEncoder.encode(labelIds, StandardCharsets.UTF_8));
            if (includeSpamTrash) url.append("&includeSpamTrash=true");

            Map<String, Object> response = restClient.get()
                    .uri(url.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] getAll failed: {}", e.getMessage());
            return ActionResult.failure("Gmail getAll failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    /**
     * Permanently delete a Gmail message.
     * Config: messageId (required)
     */
    @ActionMapping(appKey = "gmail", actionKey = "delete")
    public ActionResult delete(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String messageId = require(config, "messageId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");

        logger.info("[gmail] delete: messageId='{}'", messageId);

        try {
            restClient.delete()
                    .uri(BASE + "/" + messageId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            return ActionResult.success(Map.of("deleted", true, "id", messageId));
        } catch (Exception e) {
            logger.error("[gmail] delete failed: {}", e.getMessage());
            return ActionResult.failure("Gmail delete failed: " + e.getMessage());
        }
    }

    // ── markAsRead ────────────────────────────────────────────────────────────

    /**
     * Mark a Gmail message as read (removes UNREAD label).
     * Config: messageId (required)
     */
    @ActionMapping(appKey = "gmail", actionKey = "markAsRead")
    public ActionResult markAsRead(ActionContext context) {
        return modifyLabels(context, "markAsRead", List.of(), List.of("UNREAD"));
    }

    // ── markAsUnread ──────────────────────────────────────────────────────────

    /**
     * Mark a Gmail message as unread (adds UNREAD label).
     * Config: messageId (required)
     */
    @ActionMapping(appKey = "gmail", actionKey = "markAsUnread")
    public ActionResult markAsUnread(ActionContext context) {
        return modifyLabels(context, "markAsUnread", List.of("UNREAD"), List.of());
    }

    // ── addLabels ─────────────────────────────────────────────────────────────

    /**
     * Add labels to a Gmail message.
     * Config: messageId (required), labelIds (comma-separated or List, required)
     */
    @ActionMapping(appKey = "gmail", actionKey = "addLabels")
    public ActionResult addLabels(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String messageId = require(config, "messageId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");
        List<String> labelIds = resolveLabels(config, "labelIds");
        if (labelIds.isEmpty()) return ActionResult.failure("'labelIds' is required");

        logger.info("[gmail] addLabels: messageId='{}', labels={}", messageId, labelIds);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(BASE + "/" + messageId + "/modify")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("addLabelIds", labelIds))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] addLabels failed: {}", e.getMessage());
            return ActionResult.failure("Gmail addLabels failed: " + e.getMessage());
        }
    }

    // ── removeLabels ──────────────────────────────────────────────────────────

    /**
     * Remove labels from a Gmail message.
     * Config: messageId (required), labelIds (comma-separated or List, required)
     */
    @ActionMapping(appKey = "gmail", actionKey = "removeLabels")
    public ActionResult removeLabels(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String messageId = require(config, "messageId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");
        List<String> labelIds = resolveLabels(config, "labelIds");
        if (labelIds.isEmpty()) return ActionResult.failure("'labelIds' is required");

        logger.info("[gmail] removeLabels: messageId='{}', labels={}", messageId, labelIds);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(BASE + "/" + messageId + "/modify")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("removeLabelIds", labelIds))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] removeLabels failed: {}", e.getMessage());
            return ActionResult.failure("Gmail removeLabels failed: " + e.getMessage());
        }
    }

    // ── search ────────────────────────────────────────────────────────────────

    /**
     * Search Gmail messages using Gmail query syntax.
     * Config: q (required), maxResults (int, default 10),
     *         labelIds, includeSpamTrash (bool)
     */
    @ActionMapping(appKey = "gmail", actionKey = "search")
    @SuppressWarnings("unchecked")
    public ActionResult search(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String q = opt(config, "q", "");
        int maxResults = parseIntOpt(config, "maxResults", 10);
        String labelIds = opt(config, "labelIds", null);
        boolean includeSpamTrash = Boolean.parseBoolean(opt(config, "includeSpamTrash", "false"));

        logger.info("[gmail] search: q='{}', maxResults={}", q, maxResults);

        try {
            StringBuilder url = new StringBuilder(BASE);
            url.append("?maxResults=").append(maxResults);
            if (q != null && !q.isBlank()) url.append("&q=").append(URLEncoder.encode(q, StandardCharsets.UTF_8));
            if (labelIds != null && !labelIds.isBlank()) url.append("&labelIds=").append(URLEncoder.encode(labelIds, StandardCharsets.UTF_8));
            if (includeSpamTrash) url.append("&includeSpamTrash=true");

            Map<String, Object> response = restClient.get()
                    .uri(url.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] search failed: {}", e.getMessage());
            return ActionResult.failure("Gmail search failed: " + e.getMessage());
        }
    }

    // ── shared helpers ────────────────────────────────────────────────────────

    private ActionResult modifyLabels(ActionContext context, String op,
                                       List<String> add, List<String> remove) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String messageId = require(config, "messageId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");

        logger.info("[gmail] {}: messageId='{}'", op, messageId);

        try {
            Map<String, Object> body = new HashMap<>();
            if (!add.isEmpty()) body.put("addLabelIds", add);
            if (!remove.isEmpty()) body.put("removeLabelIds", remove);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(BASE + "/" + messageId + "/modify")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[gmail] {} failed: {}", op, e.getMessage());
            return ActionResult.failure("Gmail " + op + " failed: " + e.getMessage());
        }
    }

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

    @SuppressWarnings("unchecked")
    private List<String> resolveLabels(Map<String, Object> config, String key) {
        Object v = config.get(key);
        if (v instanceof List<?> list) return (List<String>) list;
        if (v instanceof String s && !s.isBlank()) {
            List<String> result = new ArrayList<>();
            for (String part : s.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) result.add(trimmed);
            }
            return result;
        }
        return List.of();
    }

    private String encodeRaw(List<String> lines) {
        String raw = String.join("\r\n", lines);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void appendQ(StringBuilder q, String clause) {
        if (!q.isEmpty()) q.append(" ");
        q.append(clause);
    }
}
