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
import java.util.List;
import java.util.Map;

/**
 * Searches for a Google Calendar event by query text (events.list with q parameter).
 */
@ActionMapping(appKey = "google-calendar", actionKey = "find-event")
public class GoogleCalendarFindEventHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarFindEventHandler.class);
    private static final String CALENDAR_API = "https://www.googleapis.com/calendar/v3/calendars";

    private final RestClient restClient;

    public GoogleCalendarFindEventHandler() {
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
        if (calendarId == null) calendarId = "primary";

        String query = str(config, "query");
        String timeMin = str(config, "timeMin");
        String maxResults = str(config, "maxResults");
        if (maxResults == null) maxResults = "10";

        logger.info("[google-calendar] Finding events in calendar '{}', query='{}'", calendarId, query);

        try {
            StringBuilder url = new StringBuilder(CALENDAR_API + "/" + calendarId + "/events?maxResults=" + maxResults + "&singleEvents=true&orderBy=startTime");
            if (query != null && !query.isBlank()) {
                url.append("&q=").append(java.net.URLEncoder.encode(query, "UTF-8"));
            }
            if (timeMin != null && !timeMin.isBlank()) {
                url.append("&timeMin=").append(java.net.URLEncoder.encode(timeMin, "UTF-8"));
            }

            Map<String, Object> response = restClient.get()
                    .uri(url.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> items = response != null ? (List<Map<String, Object>>) response.get("items") : List.of();

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-calendar");
            output.put("action", "find-event");
            output.put("resultCount", items != null ? items.size() : 0);
            output.put("events", items);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-calendar] Find event failed: {}", e.getMessage());
            return ActionResult.failure("Google Calendar find-event failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
