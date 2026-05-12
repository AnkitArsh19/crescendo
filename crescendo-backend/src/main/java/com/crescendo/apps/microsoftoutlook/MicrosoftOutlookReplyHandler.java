package com.crescendo.apps.microsoftoutlook;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Replies to an Outlook email message via Microsoft Graph API.
 */
@ActionMapping(appKey = "microsoft-outlook", actionKey = "reply-email")
public class MicrosoftOutlookReplyHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftOutlookReplyHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0/me/messages/";
    private final RestClient restClient = RestClient.create();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Outlook requires an OAuth2 accessToken");
        }

        String messageId = str(config, "messageId");
        String comment = str(config, "comment");
        if (messageId == null) return ActionResult.failure("'messageId' is required");
        if (comment == null) return ActionResult.failure("'comment' is required");

        logger.info("[outlook] Replying to message '{}'", messageId);

        try {
            String response = restClient.post()
                    .uri(GRAPH_API + messageId + "/reply")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("comment", comment))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-outlook");
            output.put("action", "reply-email");
            output.put("messageId", messageId);
            output.put("replied", true);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[outlook] Reply failed: {}", e.getMessage());
            return ActionResult.failure("Outlook reply failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
