package com.crescendo.apps.gmail;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Replies to an existing email thread via Gmail API (users.messages.send with threadId + In-Reply-To header).
 */
@ActionMapping(appKey = "gmail", actionKey = "reply-email")
public class GmailReplyHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GmailReplyHandler.class);
    private static final String GMAIL_SEND_API = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
    private static final String GMAIL_MSG_API = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";

    private final RestClient restClient;

    public GmailReplyHandler() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Gmail requires an OAuth2 accessToken");
        }

        String messageId = str(config, "messageId");
        String body = str(config, "body");
        if (messageId == null) return ActionResult.failure("'messageId' is required");
        if (body == null) return ActionResult.failure("'body' is required");

        logger.info("[gmail] Replying to message: id='{}'", messageId);

        try {
            // Fetch original message to get threadId, From, and Subject
            String msgUrl = GMAIL_MSG_API + messageId + "?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=Message-Id";
            Map<String, Object> original = restClient.get()
                    .uri(msgUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            String threadId = (String) original.get("threadId");
            String toAddress = "";
            String subject = "";
            String originalMsgId = "";

            var payload = (Map<String, Object>) original.get("payload");
            if (payload != null) {
                var headers = (java.util.List<Map<String, String>>) payload.get("headers");
                if (headers != null) {
                    for (var h : headers) {
                        String name = h.get("name");
                        if ("From".equalsIgnoreCase(name)) toAddress = h.get("value");
                        if ("Subject".equalsIgnoreCase(name)) subject = h.get("value");
                        if ("Message-Id".equalsIgnoreCase(name)) originalMsgId = h.get("value");
                    }
                }
            }

            if (!subject.toLowerCase().startsWith("re:")) {
                subject = "Re: " + subject;
            }

            StringBuilder rawMessage = new StringBuilder();
            rawMessage.append("To: ").append(toAddress).append("\r\n");
            rawMessage.append("Subject: ").append(subject).append("\r\n");
            rawMessage.append("Content-Type: text/html; charset=UTF-8\r\n");
            if (!originalMsgId.isEmpty()) {
                rawMessage.append("In-Reply-To: ").append(originalMsgId).append("\r\n");
                rawMessage.append("References: ").append(originalMsgId).append("\r\n");
            }
            rawMessage.append("\r\n");
            rawMessage.append(body);

            String encodedMessage = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(rawMessage.toString().getBytes(StandardCharsets.UTF_8));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("raw", encodedMessage);
            if (threadId != null) requestBody.put("threadId", threadId);

            String response = restClient.post()
                    .uri(GMAIL_SEND_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "gmail");
            output.put("action", "reply-email");
            output.put("replyTo", toAddress);
            output.put("subject", subject);
            output.put("threadId", threadId);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[gmail] Failed to reply to message {}: {}", messageId, e.getMessage());
            return ActionResult.failure("Gmail reply failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
