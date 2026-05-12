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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates a draft email in Outlook via Microsoft Graph API.
 */
@ActionMapping(appKey = "microsoft-outlook", actionKey = "create-draft")
public class MicrosoftOutlookCreateDraftHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftOutlookCreateDraftHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0/me/messages";
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

        String to = str(config, "to");
        String subject = str(config, "subject");
        String bodyHtml = str(config, "bodyHtml");
        if (to == null) return ActionResult.failure("'to' is required");
        if (subject == null) return ActionResult.failure("'subject' is required");

        logger.info("[outlook] Creating draft: to='{}', subject='{}'", to, subject);

        try {
            List<Map<String, Object>> toRecipients = Arrays.stream(to.split(","))
                    .map(String::trim)
                    .map(email -> Map.<String, Object>of("emailAddress", Map.of("address", email)))
                    .collect(Collectors.toList());

            Map<String, Object> body = new HashMap<>();
            body.put("subject", subject);
            body.put("toRecipients", toRecipients);
            body.put("body", Map.of("contentType", "HTML", "content", bodyHtml != null ? bodyHtml : ""));
            body.put("isDraft", true);

            Map<String, Object> response = restClient.post()
                    .uri(GRAPH_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-outlook");
            output.put("action", "create-draft");
            output.put("messageId", response != null ? response.get("id") : null);
            output.put("subject", subject);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[outlook] Create draft failed: {}", e.getMessage());
            return ActionResult.failure("Outlook create-draft failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
