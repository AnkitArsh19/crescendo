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

@ActionMapping(appKey = "google-calendar", actionKey = "create-event")
public class GoogleCalendarCreateEventHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarCreateEventHandler.class);
    private static final String CALENDAR_API = "https://www.googleapis.com/calendar/v3/calendars";

    private final RestClient restClient;

    public GoogleCalendarCreateEventHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? asString(creds.get("accessToken")) : null;
        if (accessToken == null || accessToken.isBlank()) {
            logger.warn("[google-calendar] create-event: missing accessToken");
            return ActionResult.failure("Google Calendar requires an 'accessToken' in connection credentials");
        }

        String summary = asString(config.get("summary"));
        String start = asString(config.get("start"));
        String end = asString(config.get("end"));
        if (summary == null || summary.isBlank()) return ActionResult.failure("'summary' is required");
        if (start == null || start.isBlank()) return ActionResult.failure("'start' is required");
        if (end == null || end.isBlank()) return ActionResult.failure("'end' is required");

        String calendarId = defaultIfBlank(asString(config.get("calendarId")), "primary");
        String timeZone = defaultIfBlank(asString(config.get("timeZone")), "UTC");

        Map<String, Object> body = new HashMap<>();
        body.put("summary", summary);
        if (config.get("description") != null) {
            body.put("description", config.get("description"));
        }
        body.put("start", Map.of("dateTime", start, "timeZone", timeZone));
        body.put("end", Map.of("dateTime", end, "timeZone", timeZone));

        logger.info("[google-calendar] Creating event: summary='{}', calendar='{}', start='{}'", summary, calendarId, start);

        try {
            String response = restClient.post()
                    .uri(CALENDAR_API + "/" + calendarId + "/events")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-calendar");
            output.put("calendarId", calendarId);
            output.put("response", response);
            logger.info("[google-calendar] Event created successfully in calendar='{}'", calendarId);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-calendar] Failed to create event", e);
            return ActionResult.failure("Google Calendar create event failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}