package com.crescendo.apps.calendly;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Calendly resources: event types and scheduled events.
 * Authenticates via OAuth2 access token.
 */
@Component
@SuppressWarnings("unchecked")
public class CalendlyResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(CalendlyResourceProvider.class);
    private static final String CALENDLY_API = "https://api.calendly.com";

    private final RestClient restClient;

    public CalendlyResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "calendly";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("event_types", "events");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("event_types", 50, java.time.Duration.ofMinutes(10)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String token = getToken(credentials);
        return switch (resourceType) {
            case "event_types" -> listEventTypes(token);
            case "events"      -> listScheduledEvents(token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listEventTypes(String token) {
        try {
            // Get current user first
            Map<String, Object> userResp = restClient.get()
                    .uri(CALENDLY_API + "/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (userResp == null) return List.of();
            Map<String, Object> resource = (Map<String, Object>) userResp.get("resource");
            if (resource == null) return List.of();
            String userUri = str(resource.get("uri"));

            Map<String, Object> etResp = restClient.get()
                    .uri(CALENDLY_API + "/event_types?user=" + userUri + "&count=100")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (etResp == null) return List.of();
            List<Map<String, Object>> collection = (List<Map<String, Object>>) etResp.get("collection");
            if (collection == null) return List.of();

            return collection.stream().map(et -> {
                String uri = str(et.get("uri"));
                String name = str(et.get("name"));
                String slug = str(et.get("slug"));
                Object duration = et.get("duration");
                return new ResourceOption(uri, name, duration != null ? duration + " mins · /" + slug : "/" + slug);
            }).toList();
        } catch (Exception e) {
            logger.error("[calendly] Failed to list event types: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listScheduledEvents(String token) {
        try {
            Map<String, Object> userResp = restClient.get()
                    .uri(CALENDLY_API + "/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (userResp == null) return List.of();
            Map<String, Object> resource = (Map<String, Object>) userResp.get("resource");
            if (resource == null) return List.of();
            String userUri = str(resource.get("uri"));

            Map<String, Object> evResp = restClient.get()
                    .uri(CALENDLY_API + "/scheduled_events?user=" + userUri + "&count=50&status=active")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (evResp == null) return List.of();
            List<Map<String, Object>> collection = (List<Map<String, Object>>) evResp.get("collection");
            if (collection == null) return List.of();

            return collection.stream().map(ev -> {
                String uri = str(ev.get("uri"));
                String name = str(ev.get("name"));
                String startTime = str(ev.get("start_time"));
                return new ResourceOption(uri, name, startTime);
            }).toList();
        } catch (Exception e) {
            logger.error("[calendly] Failed to list scheduled events: {}", e.getMessage());
            return List.of();
        }
    }

    private String getToken(Map<String, Object> credentials) {
        if (credentials != null) {
            for (String key : new String[]{"accessToken", "apiKey", "token"}) {
                Object val = credentials.get(key);
                if (val != null && !val.toString().isBlank()) return val.toString();
            }
        }
        throw new IllegalArgumentException("Calendly connection requires an access token.");
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
