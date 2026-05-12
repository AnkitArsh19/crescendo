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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Adds attendees to a Google Calendar event via events.patch.
 */
@ActionMapping(appKey = "google-calendar", actionKey = "add-attendee")
public class GoogleCalendarAddAttendeeHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarAddAttendeeHandler.class);
    private static final String CALENDAR_API = "https://www.googleapis.com/calendar/v3/calendars";

    private final RestClient restClient;

    public GoogleCalendarAddAttendeeHandler() {
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
        String attendees = str(config, "attendees");
        if (calendarId == null) calendarId = "primary";
        if (eventId == null) return ActionResult.failure("'eventId' is required");
        if (attendees == null || attendees.isBlank()) return ActionResult.failure("'attendees' is required");

        logger.info("[google-calendar] Adding attendees to event '{}': {}", eventId, attendees);

        try {
            // Fetch existing attendees first
            String getUrl = CALENDAR_API + "/" + calendarId + "/events/" + eventId;
            Map<String, Object> existing = restClient.get()
                    .uri(getUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> existingAttendees = new ArrayList<>();
            if (existing != null && existing.containsKey("attendees")) {
                existingAttendees.addAll((List<Map<String, Object>>) existing.get("attendees"));
            }

            // Parse new attendees (comma-separated emails)
            String[] emails = attendees.split(",");
            for (String email : emails) {
                String trimmed = email.trim();
                if (!trimmed.isEmpty()) {
                    existingAttendees.add(Map.of("email", trimmed));
                }
            }

            // Patch the event
            Map<String, Object> response = restClient.patch()
                    .uri(getUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("attendees", existingAttendees))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-calendar");
            output.put("action", "add-attendee");
            output.put("eventId", eventId);
            output.put("addedAttendees", attendees);
            output.put("totalAttendees", existingAttendees.size());
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-calendar] Add attendee failed: {}", e.getMessage());
            return ActionResult.failure("Google Calendar add-attendee failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
