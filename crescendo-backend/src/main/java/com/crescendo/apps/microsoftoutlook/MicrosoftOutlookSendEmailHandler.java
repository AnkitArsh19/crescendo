package com.crescendo.apps.microsoftoutlook;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "microsoft-outlook", actionKey = "send-email")
public class MicrosoftOutlookSendEmailHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftOutlookSendEmailHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        logger.info("[microsoft-outlook] Executing send-email action");
        logger.debug("[microsoft-outlook] Config: {}", config);

        String accessToken = asString(creds != null ? creds.get("accessToken") : null);
        if (accessToken == null || accessToken.isBlank()) {
            logger.error("[microsoft-outlook] No accessToken in credentials");
            return ActionResult.failure("Microsoft Outlook requires 'accessToken' in connection credentials");
        }
        logger.debug("[microsoft-outlook] accessToken present, length={}", accessToken.length());

        String to = asString(config.get("to"));
        String subject = asString(config.get("subject"));
        String bodyHtml = asString(config.get("bodyHtml"));

        if (to == null || to.isBlank()) return ActionResult.failure("'to' is required");
        if (subject == null || subject.isBlank()) return ActionResult.failure("'subject' is required");
        if (bodyHtml == null || bodyHtml.isBlank()) return ActionResult.failure("'bodyHtml' is required");

        logger.info("[microsoft-outlook] Sending email: to='{}', subject='{}'", to, subject);

        try {
            Map<String, Object> message = Map.of(
                    "subject", subject,
                    "body", Map.of(
                            "contentType", "HTML",
                            "content", bodyHtml
                    ),
                    "toRecipients", java.util.List.of(Map.of(
                            "emailAddress", Map.of("address", to)
                    ))
            );

            // Add importance if specified
            String importance = asString(config.get("importance"));
            if (importance != null && !importance.isBlank()) {
                Map<String, Object> msgWithImportance = new HashMap<>(message);
                msgWithImportance.put("importance", importance);
                message = msgWithImportance;
            }

            Map<String, Object> requestBody = Map.of("message", message, "saveToSentItems", true);
            logger.debug("[microsoft-outlook] Request body: {}", requestBody);

            ResponseEntity<String> response = RestClient.create()
                    .post()
                    .uri(GRAPH_API + "/me/sendMail")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);

            logger.info("[microsoft-outlook] Graph API response: status={}, body='{}'",
                    response.getStatusCode(), response.getBody());

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-outlook");
            output.put("statusCode", response.getStatusCode().value());
            output.put("response", response.getBody());
            output.put("sentTo", to);
            output.put("subject", subject);
            logger.info("[outlook] Email sent successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[microsoft-outlook] Send email failed: {} — {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ActionResult.failure("Microsoft Outlook send email failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
