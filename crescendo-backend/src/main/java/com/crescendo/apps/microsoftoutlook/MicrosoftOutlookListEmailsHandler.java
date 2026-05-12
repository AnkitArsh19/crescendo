package com.crescendo.apps.microsoftoutlook;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Lists recent emails from a mail folder via Microsoft Graph API.
 */
@ActionMapping(appKey = "microsoft-outlook", actionKey = "list-emails")
public class MicrosoftOutlookListEmailsHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftOutlookListEmailsHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0/me";
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

        String folderId = str(config, "folderId");
        if (folderId == null) return ActionResult.failure("'folderId' is required");

        String maxResults = str(config, "maxResults");
        if (maxResults == null) maxResults = "10";

        logger.info("[outlook] Listing emails from folder '{}'", folderId);

        try {
            String url = GRAPH_API + "/mailFolders/" + folderId + "/messages"
                    + "?$top=" + maxResults
                    + "&$select=id,subject,from,receivedDateTime,isRead,bodyPreview"
                    + "&$orderby=receivedDateTime desc";

            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> messages = response != null
                    ? (List<Map<String, Object>>) response.get("value")
                    : List.of();

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-outlook");
            output.put("action", "list-emails");
            output.put("folderId", folderId);
            output.put("resultCount", messages.size());
            output.put("messages", messages);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[outlook] List emails failed: {}", e.getMessage());
            return ActionResult.failure("Outlook list-emails failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
