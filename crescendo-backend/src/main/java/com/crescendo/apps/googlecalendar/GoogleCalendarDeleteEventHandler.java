package com.crescendo.apps.googlecalendar;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Deletes a Google Calendar event via Calendar API v3 (events.delete).
 */
@ActionMapping(appKey = "google-calendar", actionKey = "delete-event")
public class GoogleCalendarDeleteEventHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarDeleteEventHandler.class);
    private static final String CALENDAR_API = "https://www.googleapis.com/calendar/v3/calendars";

    private final RestClient restClient;

    public GoogleCalendarDeleteEventHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Calendar requires an OAuth2 accessToken");
        }

        String calendarId = str(config, "calendarId");
        String eventId = str(config, "eventId");
        if (calendarId == null) calendarId = "primary";
        if (eventId == null) return ActionResult.failure("'eventId' is required");

        logger.info("[google-calendar] Deleting event '{}' from calendar '{}'", eventId, calendarId);

        try {
            String url = CALENDAR_API + "/" + calendarId + "/events/" + eventId;
            restClient.delete()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-calendar");
            output.put("action", "delete-event");
            output.put("eventId", eventId);
            output.put("deleted", true);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-calendar] Delete event failed: {}", e.getMessage());
            return ActionResult.failure("Google Calendar delete-event failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
