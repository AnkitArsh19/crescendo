package com.crescendo.apps.gmail;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Marks a Gmail message as read by removing the UNREAD label.
 */
@ActionMapping(appKey = "gmail", actionKey = "mark-read")
public class GmailMarkReadHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GmailMarkReadHandler.class);
    private static final String GMAIL_API = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";

    private final RestClient restClient;

    public GmailMarkReadHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Gmail requires an OAuth2 accessToken");
        }

        String messageId = str(config, "messageId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");

        logger.info("[gmail] Marking message '{}' as read", messageId);

        try {
            String url = GMAIL_API + messageId + "/modify";
            String response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(Map.of("removeLabelIds", List.of("UNREAD")))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "gmail");
            output.put("action", "mark-read");
            output.put("messageId", messageId);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[gmail] Failed to mark read: {}", e.getMessage());
            return ActionResult.failure("Gmail mark-read failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
