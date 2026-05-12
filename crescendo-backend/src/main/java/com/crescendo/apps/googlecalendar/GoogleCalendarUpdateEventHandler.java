package com.crescendo.apps.googlecalendar;

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
 * Updates an existing Google Calendar event via Calendar API v3 (events.patch).
 */
@ActionMapping(appKey = "google-calendar", actionKey = "update-event")
public class GoogleCalendarUpdateEventHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarUpdateEventHandler.class);
    private static final String CALENDAR_API = "https://www.googleapis.com/calendar/v3/calendars";

    private final RestClient restClient;

    public GoogleCalendarUpdateEventHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    @SuppressWarnings("unchecked")
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

        logger.info("[google-calendar] Updating event '{}' in calendar '{}'", eventId, calendarId);

        try {
            Map<String, Object> patch = new HashMap<>();
            if (config.containsKey("summary")) patch.put("summary", config.get("summary"));
            if (config.containsKey("description")) patch.put("description", config.get("description"));
            if (config.containsKey("location")) patch.put("location", config.get("location"));

            String tz = str(config, "timeZone");
            if (tz == null) tz = "UTC";

            if (config.containsKey("start")) {
                patch.put("start", Map.of("dateTime", config.get("start"), "timeZone", tz));
            }
            if (config.containsKey("end")) {
                patch.put("end", Map.of("dateTime", config.get("end"), "timeZone", tz));
            }

            String url = CALENDAR_API + "/" + calendarId + "/events/" + eventId;
            Map<String, Object> response = restClient.patch()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-calendar");
            output.put("action", "update-event");
            output.put("eventId", eventId);
            output.put("htmlLink", response != null ? response.get("htmlLink") : null);
            output.put("updated", response != null ? response.get("updated") : null);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-calendar] Update event failed: {}", e.getMessage());
            return ActionResult.failure("Google Calendar update-event failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
