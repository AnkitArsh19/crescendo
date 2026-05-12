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
 * Removes a label from a Gmail message via users.messages.modify.
 */
@ActionMapping(appKey = "gmail", actionKey = "remove-label")
public class GmailRemoveLabelHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GmailRemoveLabelHandler.class);
    private static final String GMAIL_API = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";

    private final RestClient restClient;

    public GmailRemoveLabelHandler() {
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
        String labelId = str(config, "labelId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");
        if (labelId == null) return ActionResult.failure("'labelId' is required");

        logger.info("[gmail] Removing label '{}' from message '{}'", labelId, messageId);

        try {
            String url = GMAIL_API + messageId + "/modify";
            String response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(Map.of("removeLabelIds", List.of(labelId)))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "gmail");
            output.put("action", "remove-label");
            output.put("messageId", messageId);
            output.put("labelId", labelId);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[gmail] Failed to remove label: {}", e.getMessage());
            return ActionResult.failure("Gmail remove-label failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
