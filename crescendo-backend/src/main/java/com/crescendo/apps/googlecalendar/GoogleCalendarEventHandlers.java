package com.crescendo.apps.googlecalendar;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grouped handler for Google Calendar event operations.
 *
 * <p>Operations (mirrors n8n {@code EventDescription.ts}):
 * <ul>
 *   <li>{@code create}      — events.insert</li>
 *   <li>{@code update}      — events.patch</li>
 *   <li>{@code delete}      — events.delete</li>
 *   <li>{@code get}         — events.get</li>
 *   <li>{@code getAll}      — events.list</li>
 *   <li>{@code addAttendee} — events.patch (merges attendee list)</li>
 * </ul>
 *
 * <p>Credentials: {@code accessToken} (OAuth2, calendar scopes)
 */
@Component
public class GoogleCalendarEventHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarEventHandlers.class);

    private static final String BASE = "https://www.googleapis.com/calendar/v3/calendars";

    private final RestClient restClient;

    public GoogleCalendarEventHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    /**
     * Create a calendar event.
     * Config: summary (required), start (required, ISO dateTime), end (required, ISO dateTime),
     *         calendarId (default "primary"), timeZone (default "UTC"),
     *         description, location, attendees (comma-sep emails), allDay (bool)
     */
    @ActionMapping(appKey = "googlecalendar", actionKey = "create")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String summary = require(config, "summary");
        if (summary == null) return ActionResult.failure("'summary' is required");
        String start = require(config, "start");
        if (start == null) return ActionResult.failure("'start' is required");
        String end = require(config, "end");
        if (end == null) return ActionResult.failure("'end' is required");

        String calendarId = opt(config, "calendarId", "primary");
        String timeZone = opt(config, "timeZone", "UTC");
        boolean allDay = Boolean.parseBoolean(opt(config, "allDay", "false"));

        logger.info("[googlecalendar] create: summary='{}', calendar='{}', start='{}'", summary, calendarId, start);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("summary", summary);
            String desc = opt(config, "description", null);
            if (desc != null) body.put("description", desc);
            String location = opt(config, "location", null);
            if (location != null) body.put("location", location);

            // n8n uses dateTime for timed events, date for all-day
            if (allDay) {
                body.put("start", Map.of("date", start));
                body.put("end", Map.of("date", end));
            } else {
                body.put("start", Map.of("dateTime", start, "timeZone", timeZone));
                body.put("end", Map.of("dateTime", end, "timeZone", timeZone));
            }

            // Optional attendees (comma-separated emails)
            String attendeesStr = opt(config, "attendees", null);
            if (attendeesStr != null) {
                body.put("attendees", parseAttendees(attendeesStr));
            }

            Map<String, Object> response = restClient.post()
                    .uri(BASE + "/" + calendarId + "/events")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlecalendar] create failed: {}", e.getMessage());
            return ActionResult.failure("Google Calendar create failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    /**
     * Update a calendar event (PATCH — only provided fields are changed).
     * Config: eventId (required), calendarId (default "primary"),
     *         summary, description, location, start, end, timeZone
     */
    @ActionMapping(appKey = "googlecalendar", actionKey = "update")
    @SuppressWarnings("unchecked")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String eventId = require(config, "eventId");
        if (eventId == null) return ActionResult.failure("'eventId' is required");
        String calendarId = opt(config, "calendarId", "primary");
        String timeZone = opt(config, "timeZone", "UTC");

        logger.info("[googlecalendar] update: eventId='{}', calendar='{}'", eventId, calendarId);

        try {
            Map<String, Object> patch = new HashMap<>();
            if (config.containsKey("summary")) patch.put("summary", config.get("summary"));
            if (config.containsKey("description")) patch.put("description", config.get("description"));
            if (config.containsKey("location")) patch.put("location", config.get("location"));
            if (config.containsKey("start")) patch.put("start", Map.of("dateTime", config.get("start"), "timeZone", timeZone));
            if (config.containsKey("end")) patch.put("end", Map.of("dateTime", config.get("end"), "timeZone", timeZone));

            Map<String, Object> response = restClient.patch()
                    .uri(BASE + "/" + calendarId + "/events/" + eventId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlecalendar] update failed: {}", e.getMessage());
            return ActionResult.failure("Google Calendar update failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    /**
     * Delete a calendar event.
     * Config: eventId (required), calendarId (default "primary"),
     *         sendUpdates (none|all|externalOnly — notify attendees)
     */
    @ActionMapping(appKey = "googlecalendar", actionKey = "delete")
    public ActionResult delete(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String eventId = require(config, "eventId");
        if (eventId == null) return ActionResult.failure("'eventId' is required");
        String calendarId = opt(config, "calendarId", "primary");
        String sendUpdates = opt(config, "sendUpdates", "none");

        logger.info("[googlecalendar] delete: eventId='{}', calendar='{}'", eventId, calendarId);

        try {
            restClient.delete()
                    .uri(BASE + "/" + calendarId + "/events/" + eventId + "?sendUpdates=" + sendUpdates)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            return ActionResult.success(Map.of("deleted", true, "id", eventId));
        } catch (Exception e) {
            logger.error("[googlecalendar] delete failed: {}", e.getMessage());
            return ActionResult.failure("Google Calendar delete failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────

    /**
     * Get a single calendar event by ID.
     * Config: eventId (required), calendarId (default "primary")
     */
    @ActionMapping(appKey = "googlecalendar", actionKey = "get")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String eventId = require(config, "eventId");
        if (eventId == null) return ActionResult.failure("'eventId' is required");
        String calendarId = opt(config, "calendarId", "primary");

        logger.info("[googlecalendar] get: eventId='{}', calendar='{}'", eventId, calendarId);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE + "/" + calendarId + "/events/" + eventId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlecalendar] get failed: {}", e.getMessage());
            return ActionResult.failure("Google Calendar get failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    /**
     * List calendar events.
     * Config: calendarId (default "primary"), returnAll (bool), limit (int, default 50),
     *         timeMin (ISO dateTime), timeMax (ISO dateTime), query (text search),
     *         showDeleted (bool), singleEvents (bool, default true)
     */
    @ActionMapping(appKey = "googlecalendar", actionKey = "getAll")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String calendarId = opt(config, "calendarId", "primary");
        boolean returnAll = Boolean.parseBoolean(opt(config, "returnAll", "false"));
        int limit = parseIntOpt(config, "limit", 50);
        String timeMin = opt(config, "timeMin", null);
        String timeMax = opt(config, "timeMax", null);
        String query = opt(config, "query", null);
        boolean showDeleted = Boolean.parseBoolean(opt(config, "showDeleted", "false"));
        boolean singleEvents = Boolean.parseBoolean(opt(config, "singleEvents", "true"));

        logger.info("[googlecalendar] getAll: calendar='{}', limit={}", calendarId, limit);

        try {
            StringBuilder url = new StringBuilder(BASE + "/" + calendarId + "/events?");
            url.append("maxResults=").append(returnAll ? 2500 : limit);
            url.append("&singleEvents=").append(singleEvents);
            if (showDeleted) url.append("&showDeleted=true");
            if (timeMin != null) url.append("&timeMin=").append(java.net.URLEncoder.encode(timeMin, java.nio.charset.StandardCharsets.UTF_8));
            if (timeMax != null) url.append("&timeMax=").append(java.net.URLEncoder.encode(timeMax, java.nio.charset.StandardCharsets.UTF_8));
            if (query != null) url.append("&q=").append(java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8));

            Map<String, Object> response = restClient.get()
                    .uri(url.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlecalendar] getAll failed: {}", e.getMessage());
            return ActionResult.failure("Google Calendar getAll failed: " + e.getMessage());
        }
    }

    // ── addAttendee ───────────────────────────────────────────────────────────

    /**
     * Add one or more attendees to an existing calendar event.
     * Config: eventId (required), attendees (required, comma-separated emails),
     *         calendarId (default "primary"), sendUpdates (none|all|externalOnly)
     */
    @ActionMapping(appKey = "googlecalendar", actionKey = "addAttendee")
    @SuppressWarnings("unchecked")
    public ActionResult addAttendee(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String eventId = require(config, "eventId");
        if (eventId == null) return ActionResult.failure("'eventId' is required");
        String attendeesStr = require(config, "attendees");
        if (attendeesStr == null) return ActionResult.failure("'attendees' is required");
        String calendarId = opt(config, "calendarId", "primary");
        String sendUpdates = opt(config, "sendUpdates", "none");

        logger.info("[googlecalendar] addAttendee: eventId='{}', attendees='{}'", eventId, attendeesStr);

        try {
            String eventUrl = BASE + "/" + calendarId + "/events/" + eventId;

            // Fetch existing attendees first to merge
            Map<String, Object> existing = restClient.get()
                    .uri(eventUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> attendeeList = new ArrayList<>();
            if (existing != null && existing.containsKey("attendees")) {
                attendeeList.addAll((List<Map<String, Object>>) existing.get("attendees"));
            }
            attendeeList.addAll(parseAttendees(attendeesStr));

            Map<String, Object> response = restClient.patch()
                    .uri(eventUrl + "?sendUpdates=" + sendUpdates)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("attendees", attendeeList))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlecalendar] addAttendee failed: {}", e.getMessage());
            return ActionResult.failure("Google Calendar addAttendee failed: " + e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<Map<String, Object>> parseAttendees(String commaSepEmails) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String email : commaSepEmails.split(",")) {
            String trimmed = email.trim();
            if (!trimmed.isEmpty()) list.add(Map.of("email", trimmed));
        }
        return list;
    }

    private String resolveToken(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        return token != null && !token.toString().isBlank() ? token.toString() : null;
    }

    private ActionResult missingToken() {
        return ActionResult.failure("Google Calendar requires an OAuth2 accessToken in connection credentials");
    }

    private String require(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : null;
    }

    private String opt(Map<String, Object> config, String key, String defaultVal) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : defaultVal;
    }

    private int parseIntOpt(Map<String, Object> config, String key, int defaultVal) {
        Object v = config.get(key);
        if (v == null) return defaultVal;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return defaultVal; }
    }
}
