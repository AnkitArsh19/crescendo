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
 * Moves an Outlook email to a different mail folder via Microsoft Graph API.
 */
@ActionMapping(appKey = "microsoft-outlook", actionKey = "move-email")
public class MicrosoftOutlookMoveEmailHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftOutlookMoveEmailHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0/me/messages/";
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Outlook requires an OAuth2 accessToken");
        }

        String messageId = str(config, "messageId");
        String folderId = str(config, "folderId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");
        if (folderId == null) return ActionResult.failure("'folderId' is required");

        logger.info("[outlook] Moving message '{}' to folder '{}'", messageId, folderId);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(GRAPH_API + messageId + "/move")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("destinationId", folderId))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-outlook");
            output.put("action", "move-email");
            output.put("messageId", messageId);
            output.put("newFolderId", folderId);
            output.put("newMessageId", response != null ? response.get("id") : null);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[outlook] Move email failed: {}", e.getMessage());
            return ActionResult.failure("Outlook move-email failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
