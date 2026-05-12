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
 * Creates a draft email in Gmail via the Gmail REST API (users.drafts.create).
 */
@ActionMapping(appKey = "gmail", actionKey = "create-draft")
public class GmailCreateDraftHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GmailCreateDraftHandler.class);
    private static final String GMAIL_API = "https://gmail.googleapis.com/gmail/v1/users/me/drafts";

    private final RestClient restClient;

    public GmailCreateDraftHandler() {
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
            return ActionResult.failure("Gmail requires an OAuth2 accessToken");
        }

        String to = str(config, "to");
        String subject = str(config, "subject");
        String body = str(config, "body");
        if (to == null) return ActionResult.failure("'to' is required");
        if (subject == null) return ActionResult.failure("'subject' is required");
        if (body == null) return ActionResult.failure("'body' is required");

        logger.info("[gmail] Creating draft: to='{}', subject='{}'", to, subject);

        try {
            String rawMessage = String.join("\r\n",
                    "To: " + to,
                    "Subject: " + subject,
                    "Content-Type: text/html; charset=UTF-8",
                    "",
                    body
            );

            String encodedMessage = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(rawMessage.getBytes(StandardCharsets.UTF_8));

            String response = restClient.post()
                    .uri(GMAIL_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("message", Map.of("raw", encodedMessage)))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "gmail");
            output.put("action", "create-draft");
            output.put("to", to);
            output.put("subject", subject);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[gmail] Failed to create draft: {}", e.getMessage());
            return ActionResult.failure("Gmail create-draft failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
