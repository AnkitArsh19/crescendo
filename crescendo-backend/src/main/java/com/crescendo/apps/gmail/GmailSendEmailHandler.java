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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends emails via the Gmail REST API (users.messages.send).
 *
 * <p>Connection credentials: {@code accessToken} (OAuth2 with gmail.send scope)
 * <p>Config: {@code to}, {@code subject}, {@code body}
 */
@ActionMapping(appKey = "gmail", actionKey = "send-email")
public class GmailSendEmailHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GmailSendEmailHandler.class);
    private static final String GMAIL_API = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private final RestClient restClient;

    public GmailSendEmailHandler() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            logger.warn("[gmail] send-email: missing accessToken");
            return ActionResult.failure("Gmail requires an OAuth2 accessToken in connection credentials");
        }

        String to = getRequired(config, "to");
        String subject = getRequired(config, "subject");
        String body = getRequired(config, "body");
        if (to == null) return ActionResult.failure("'to' is required");
        if (subject == null) return ActionResult.failure("'subject' is required");
        if (body == null) return ActionResult.failure("'body' is required");

        logger.info("[gmail] Sending email: to='{}', subject='{}'", to, subject);

        // Optional cc / bcc (schema exposes them but handler was previously ignoring them)
        String cc = getOptional(config, "cc");
        String bcc = getOptional(config, "bcc");

        try {
            List<String> mimeLines = new ArrayList<>();
            mimeLines.add("To: " + to);
            if (cc != null && !cc.isBlank()) mimeLines.add("Cc: " + cc);
            if (bcc != null && !bcc.isBlank()) mimeLines.add("Bcc: " + bcc);
            mimeLines.add("Subject: " + subject);
            mimeLines.add("Content-Type: text/html; charset=UTF-8");
            mimeLines.add("");
            mimeLines.add(body);

            String rawMessage = String.join("\r\n", mimeLines);

            String encodedMessage = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(rawMessage.getBytes(StandardCharsets.UTF_8));

            String response = restClient.post()
                    .uri(GMAIL_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("raw", encodedMessage))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "gmail");
            output.put("to", to);
            if (cc != null && !cc.isBlank()) output.put("cc", cc);
            output.put("subject", subject);
            output.put("response", response);
            logger.info("[gmail] Email sent successfully to '{}'", to);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[gmail] Failed to send email to {}: {}", to, e.getMessage());
            return ActionResult.failure("Gmail send failed: " + e.getMessage());
        }
    }

    private String getRequired(Map<String, Object> config, String key) {
        Object val = config.get(key);
        return val != null ? val.toString() : null;
    }

    private String getOptional(Map<String, Object> config, String key) {
        Object val = config.get(key);
        return val != null && !val.toString().isBlank() ? val.toString() : null;
    }
}
