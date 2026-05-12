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
 * Creates a calendar event on the user's Outlook calendar via Microsoft Graph API.
 */
@ActionMapping(appKey = "microsoft-outlook", actionKey = "create-event")
public class MicrosoftOutlookCreateEventHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftOutlookCreateEventHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0/me/events";
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

        String subject = str(config, "subject");
        String start = str(config, "start");
        String end = str(config, "end");
        if (subject == null) return ActionResult.failure("'subject' is required");
        if (start == null) return ActionResult.failure("'start' is required");
        if (end == null) return ActionResult.failure("'end' is required");

        String timeZone = str(config, "timeZone");
        if (timeZone == null) timeZone = "UTC";
        String body = str(config, "body");
        String attendeesStr = str(config, "attendees");

        logger.info("[outlook] Creating event: subject='{}', start='{}'", subject, start);

        try {
            Map<String, Object> eventBody = new HashMap<>();
            eventBody.put("subject", subject);
            eventBody.put("start", Map.of("dateTime", start, "timeZone", timeZone));
            eventBody.put("end", Map.of("dateTime", end, "timeZone", timeZone));

            if (body != null) {
                eventBody.put("body", Map.of("contentType", "HTML", "content", body));
            }

            if (attendeesStr != null && !attendeesStr.isBlank()) {
                List<Map<String, Object>> attendees = Arrays.stream(attendeesStr.split(","))
                        .map(String::trim)
                        .map(email -> Map.<String, Object>of(
                                "emailAddress", Map.of("address", email),
                                "type", "required"
                        ))
                        .collect(Collectors.toList());
                eventBody.put("attendees", attendees);
            }

            Map<String, Object> response = restClient.post()
                    .uri(GRAPH_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(eventBody)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-outlook");
            output.put("action", "create-event");
            output.put("eventId", response != null ? response.get("id") : null);
            output.put("webLink", response != null ? response.get("webLink") : null);
            output.put("subject", subject);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[outlook] Create event failed: {}", e.getMessage());
            return ActionResult.failure("Outlook create-event failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
