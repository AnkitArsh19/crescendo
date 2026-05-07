package com.crescendo.apps.googlecalendar;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "google-calendar", actionKey = "list-events")
public class GoogleCalendarListEventsHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarListEventsHandler.class);
    private static final String CALENDAR_API = "https://www.googleapis.com/calendar/v3/calendars";

    private final RestClient restClient;

    public GoogleCalendarListEventsHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? asString(creds.get("accessToken")) : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Calendar requires an 'accessToken' in connection credentials");
        }

        String calendarId = defaultIfBlank(asString(config.get("calendarId")), "primary");
        String timeMin = defaultIfBlank(asString(config.get("timeMin")), Instant.now().toString());
        int maxResults = parseInt(config.get("maxResults"), 10);

        try {
            String uri = CALENDAR_API + "/" + calendarId + "/events?singleEvents=true&orderBy=startTime"
                    + "&maxResults=" + maxResults
                    + "&timeMin=" + URLEncoder.encode(timeMin, StandardCharsets.UTF_8);

            String response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-calendar");
            output.put("calendarId", calendarId);
            output.put("response", response);
            logger.info("[google-calendar] Events listed successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-calendar] Failed to list events", e);
            return ActionResult.failure("Google Calendar list events failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}