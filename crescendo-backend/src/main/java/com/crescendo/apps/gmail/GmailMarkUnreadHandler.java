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
 * Marks a Gmail message as unread by adding the UNREAD label.
 */
@ActionMapping(appKey = "gmail", actionKey = "mark-unread")
public class GmailMarkUnreadHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GmailMarkUnreadHandler.class);
    private static final String GMAIL_API = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";

    private final RestClient restClient;

    public GmailMarkUnreadHandler() {
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

        logger.info("[gmail] Marking message '{}' as unread", messageId);

        try {
            String url = GMAIL_API + messageId + "/modify";
            String response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(Map.of("addLabelIds", List.of("UNREAD")))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "gmail");
            output.put("action", "mark-unread");
            output.put("messageId", messageId);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[gmail] Failed to mark unread: {}", e.getMessage());
            return ActionResult.failure("Gmail mark-unread failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
