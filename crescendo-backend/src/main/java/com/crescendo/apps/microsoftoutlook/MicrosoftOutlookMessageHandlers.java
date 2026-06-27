package com.crescendo.apps.microsoftoutlook;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
// import org.springframework.core.ParameterizedTypeReference;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grouped handler for Microsoft Outlook Message operations.
 */
@Component
public class MicrosoftOutlookMessageHandlers {

    private static final String GRAPH_API = MicrosoftOutlookSupport.GRAPH_API;
    private final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    // ── send ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "sendEmail")
    public ActionResult send(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String to = MicrosoftOutlookSupport.require(config, "to");
        String subject = MicrosoftOutlookSupport.require(config, "subject");
        String bodyHtml = MicrosoftOutlookSupport.require(config, "bodyHtml");

        if (to == null || subject == null || bodyHtml == null) {
            return ActionResult.failure("'to', 'subject', and 'bodyHtml' are required");
        }

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("subject", subject);
            message.put("body", Map.of("contentType", MicrosoftOutlookSupport.opt(config, "bodyContentType", "HTML"), "content", bodyHtml));
            message.put("toRecipients", parseRecipients(to));

            String cc = MicrosoftOutlookSupport.opt(config, "cc", null);
            if (cc != null) message.put("ccRecipients", parseRecipients(cc));

            String bcc = MicrosoftOutlookSupport.opt(config, "bcc", null);
            if (bcc != null) message.put("bccRecipients", parseRecipients(bcc));

            String replyTo = MicrosoftOutlookSupport.opt(config, "replyTo", null);
            if (replyTo != null) message.put("replyTo", parseRecipients(replyTo));

            String importance = MicrosoftOutlookSupport.opt(config, "importance", null);
            if (importance != null) message.put("importance", importance);

            String isReadReceiptRequested = MicrosoftOutlookSupport.opt(config, "isReadReceiptRequested", null);
            if (isReadReceiptRequested != null) message.put("isReadReceiptRequested", Boolean.parseBoolean(isReadReceiptRequested));

            String customHeadersStr = MicrosoftOutlookSupport.opt(config, "customHeaders", null);
            if (customHeadersStr != null) {
                try {
                    Map<String, String> headersMap = mapper.readValue(customHeadersStr, new tools.jackson.core.type.TypeReference<Map<String, String>>() {});
                    List<Map<String, String>> internetMessageHeaders = headersMap.entrySet().stream()
                            .map(e -> Map.of("name", e.getKey(), "value", e.getValue()))
                            .toList();
                    message.put("internetMessageHeaders", internetMessageHeaders);
                } catch (Exception ignored) { }
            }

            boolean saveToSentItems = Boolean.parseBoolean(MicrosoftOutlookSupport.opt(config, "saveToSentItems", "true"));
            Map<String, Object> requestBody = Map.of("message", message, "saveToSentItems", saveToSentItems);

            ResponseEntity<String> response = MicrosoftOutlookSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/me/sendMail")
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("statusCode", response.getStatusCode().value());
            output.put("response", response.getBody());
            return ActionResult.success(output);
        } catch (Exception e) {
            return ActionResult.failure("Outlook sendEmail failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "getMessage")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String messageId = MicrosoftOutlookSupport.require(context.configuration(), "messageId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/messages/" + messageId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook getMessage failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "getAllMessages")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        int maxResults = MicrosoftOutlookSupport.parseIntOpt(config, "maxResults", 50);
        String folderId = MicrosoftOutlookSupport.opt(config, "folderId", null);

        try {
            String uri = GRAPH_API + "/me/" + (folderId != null ? "mailFolders/" + folderId + "/messages" : "messages") + "?$top=" + maxResults;
            
            String search = MicrosoftOutlookSupport.opt(config, "search", null);
            if (search != null) uri += "&$search=\"" + search + "\"";
            
            String filter = MicrosoftOutlookSupport.opt(config, "filter", null);
            if (filter != null) uri += "&$filter=" + filter;

            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook getAllMessages failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "deleteMessage")
    public ActionResult delete(ActionContext context) {
        String messageId = MicrosoftOutlookSupport.require(context.configuration(), "messageId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");

        try {
            MicrosoftOutlookSupport.clientBuilder(context).build().delete()
                    .uri(GRAPH_API + "/me/messages/" + messageId)
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "messageId", messageId));
        } catch (Exception e) {
            return ActionResult.failure("Outlook deleteMessage failed: " + e.getMessage());
        }
    }

    // ── move ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "moveMessage")
    @SuppressWarnings("unchecked")
    public ActionResult move(ActionContext context) {
        String messageId = MicrosoftOutlookSupport.require(context.configuration(), "messageId");
        String destinationId = MicrosoftOutlookSupport.require(context.configuration(), "destinationId");
        if (messageId == null || destinationId == null) return ActionResult.failure("'messageId' and 'destinationId' are required");

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/me/messages/" + messageId + "/move")
                    .body(Map.of("destinationId", destinationId))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook moveMessage failed: " + e.getMessage());
        }
    }

    // ── reply ─────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "replyMessage")
    public ActionResult reply(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String messageId = MicrosoftOutlookSupport.require(config, "messageId");
        String comment = MicrosoftOutlookSupport.require(config, "comment");
        if (messageId == null || comment == null) return ActionResult.failure("'messageId' and 'comment' are required");

        try {
            boolean replyAll = Boolean.parseBoolean(MicrosoftOutlookSupport.opt(config, "replyAll", "false"));
            String endpoint = replyAll ? "/replyAll" : "/reply";

            Map<String, Object> body = Map.of(
                    "message", Map.of(
                            "body", Map.of("contentType", "HTML", "content", comment)
                    )
            );

            MicrosoftOutlookSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/me/messages/" + messageId + endpoint)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            return ActionResult.success(Map.of("success", true, "messageId", messageId));
        } catch (Exception e) {
            return ActionResult.failure("Outlook replyMessage failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "updateMessage")
    @SuppressWarnings("unchecked")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String messageId = MicrosoftOutlookSupport.require(config, "messageId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");

        try {
            Map<String, Object> patch = new HashMap<>();
            String isRead = MicrosoftOutlookSupport.opt(config, "isRead", null);
            if (isRead != null) patch.put("isRead", Boolean.parseBoolean(isRead));

            String flag = MicrosoftOutlookSupport.opt(config, "flagStatus", null); // notFlagged, complete, flagged
            if (flag != null) patch.put("flag", Map.of("flagStatus", flag));

            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().patch()
                    .uri(GRAPH_API + "/me/messages/" + messageId)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook updateMessage failed: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> parseRecipients(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(addr -> !addr.isBlank())
                .map(addr -> Map.<String, Object>of("emailAddress", Map.of("address", addr)))
                .toList();
    }
}
