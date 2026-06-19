package com.crescendo.apps.googlecalendar;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Google Calendar resources for dynamic dropdowns.
 * Supports: calendars, events (depends on calendarId)
 */
@Component
@SuppressWarnings("unchecked")
public class GoogleCalendarResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarResourceProvider.class);
    private static final String CALENDAR_API = "https://www.googleapis.com/calendar/v3";

    @Override
    public String appKey() {
        return "google-calendar";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("calendars", "events");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = credentials.get("accessToken").toString();

        return switch (resourceType) {
            case "calendars" -> listCalendars(accessToken);
            case "events" -> listEvents(accessToken, params.get("calendarId"));
            default -> List.of();
        };
    }

    private List<ResourceOption> listCalendars(String accessToken) {
        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(CALENDAR_API + "/users/me/calendarList")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null) return List.of();

            return items.stream()
                    .map(cal -> new ResourceOption(
                            cal.get("id").toString(),
                            cal.get("summary").toString(),
                            cal.get("description") != null ? cal.get("description").toString() : null
                    ))
                    .toList();
        } catch (Exception e) {
            logger.error("[google-calendar] Failed to list calendars: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listEvents(String accessToken, String calendarId) {
        if (calendarId == null || calendarId.isBlank()) return List.of();

        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(CALENDAR_API + "/calendars/{calendarId}/events?maxResults=50&orderBy=startTime&singleEvents=true&timeMin={timeMin}",
                            calendarId, java.time.Instant.now().toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null) return List.of();

            return items.stream()
                    .map(event -> new ResourceOption(
                            event.get("id").toString(),
                            event.get("summary") != null ? event.get("summary").toString() : "(No title)",
                            null
                    ))
                    .toList();
        } catch (Exception e) {
            logger.error("[google-calendar] Failed to list events: {}", e.getMessage());
            return List.of();
        }
    }

    private RestClient restClient(String accessToken) {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
