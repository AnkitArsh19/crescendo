package com.crescendo.apps.calcom;

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
 * Fetches Cal.com resources: event types and bookings.
 * Authenticates via API Key.
 */
@Component
@SuppressWarnings("unchecked")
public class CalComResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(CalComResourceProvider.class);
    private static final String CALCOM_API_V1 = "https://api.cal.com/v1";
    private static final String CALCOM_API_V2 = "https://api.cal.com/v2";

    private final RestClient restClient;

    public CalComResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "calcom";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("event_types", "bookings", "schedules");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("event_types", 50, java.time.Duration.ofMinutes(10)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String apiKey = getApiKey(credentials);
        return switch (resourceType) {
            case "event_types" -> listEventTypes(apiKey);
            case "bookings"    -> listBookings(apiKey);
            case "schedules"   -> listSchedules(apiKey);
            default -> List.of();
        };
    }

    private List<ResourceOption> listEventTypes(String apiKey) {
        try {
            // Try v1 first: GET /event-types?apiKey=...
            Map<String, Object> resp = restClient.get()
                    .uri(CALCOM_API_V1 + "/event-types?apiKey=" + apiKey)
                    .retrieve().body(Map.class);
            if (resp != null && resp.containsKey("event_types")) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) resp.get("event_types");
                if (list != null) {
                    return list.stream().map(et -> {
                        String id = String.valueOf(et.get("id"));
                        String title = str(et.get("title"));
                        String slug = str(et.get("slug"));
                        Object length = et.get("length");
                        return new ResourceOption(id, title.isBlank() ? "Event " + id : title,
                                length != null ? length + " mins · /" + slug : "/" + slug);
                    }).toList();
                }
            }

            // Fallback v2: GET /event-types with Bearer token
            Map<String, Object> respV2 = restClient.get()
                    .uri(CALCOM_API_V2 + "/event-types")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .retrieve().body(Map.class);
            if (respV2 != null && respV2.containsKey("data")) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) respV2.get("data");
                if (list != null) {
                    return list.stream().map(et -> {
                        String id = String.valueOf(et.get("id"));
                        String title = str(et.get("title"));
                        String slug = str(et.get("slug"));
                        return new ResourceOption(id, title.isBlank() ? "Event " + id : title, "/" + slug);
                    }).toList();
                }
            }
            return List.of();
        } catch (Exception e) {
            logger.error("[calcom] Failed to list event types: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listBookings(String apiKey) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(CALCOM_API_V1 + "/bookings?apiKey=" + apiKey)
                    .retrieve().body(Map.class);
            if (resp == null || !resp.containsKey("bookings")) return List.of();
            List<Map<String, Object>> list = (List<Map<String, Object>>) resp.get("bookings");
            if (list == null) return List.of();

            return list.stream().map(b -> {
                String id = String.valueOf(b.get("id"));
                String title = str(b.get("title"));
                String startTime = str(b.get("startTime"));
                return new ResourceOption(id, title.isBlank() ? "Booking #" + id : title, startTime);
            }).toList();
        } catch (Exception e) {
            logger.error("[calcom] Failed to list bookings: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listSchedules(String apiKey) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(CALCOM_API_V1 + "/schedules?apiKey=" + apiKey)
                    .retrieve().body(Map.class);
            if (resp == null || !resp.containsKey("schedules")) return List.of();
            List<Map<String, Object>> list = (List<Map<String, Object>>) resp.get("schedules");
            if (list == null) return List.of();

            return list.stream().map(s -> {
                String id = String.valueOf(s.get("id"));
                String name = str(s.get("name"));
                String tz = str(s.get("timeZone"));
                return new ResourceOption(id, name.isBlank() ? "Schedule " + id : name, tz);
            }).toList();
        } catch (Exception e) {
            logger.error("[calcom] Failed to list schedules: {}", e.getMessage());
            return List.of();
        }
    }

    private String getApiKey(Map<String, Object> credentials) {
        if (credentials != null) {
            for (String key : new String[]{"apiKey", "accessToken", "token"}) {
                Object val = credentials.get(key);
                if (val != null && !val.toString().isBlank()) return val.toString();
            }
        }
        throw new IllegalArgumentException("Cal.com connection requires an API Key.");
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
