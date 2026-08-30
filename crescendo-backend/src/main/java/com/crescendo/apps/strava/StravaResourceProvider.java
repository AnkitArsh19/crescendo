package com.crescendo.apps.strava;

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
 * Fetches Strava resources: recent activities.
 * Authenticates via OAuth2 access token.
 */
@Component
@SuppressWarnings("unchecked")
public class StravaResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(StravaResourceProvider.class);
    private static final String STRAVA_API = "https://www.strava.com/api/v3";

    private final RestClient restClient;

    public StravaResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "strava";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("activities", "gear");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of();
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String token = getToken(credentials);
        return switch (resourceType) {
            case "activities" -> listActivities(token);
            case "gear"       -> listGear(token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listActivities(String token) {
        try {
            List<Map<String, Object>> activities = restClient.get()
                    .uri(STRAVA_API + "/athlete/activities?per_page=50")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(List.class);
            if (activities == null) return List.of();
            return activities.stream().map(a -> {
                String id = String.valueOf(a.get("id"));
                String name = str(a.get("name"));
                String type = str(a.get("type"));
                String date = str(a.get("start_date_local")).substring(0, Math.min(10, str(a.get("start_date_local")).length()));
                return new ResourceOption(id, name.isBlank() ? type + " " + id : name, type + " · " + date);
            }).toList();
        } catch (Exception e) {
            logger.error("[strava] Failed to list activities: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listGear(String token) {
        try {
            Map<String, Object> athlete = restClient.get()
                    .uri(STRAVA_API + "/athlete")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (athlete == null) return List.of();
            List<ResourceOption> options = new ArrayList<>();
            List<Map<String, Object>> bikes = (List<Map<String, Object>>) athlete.getOrDefault("bikes", List.of());
            List<Map<String, Object>> shoes = (List<Map<String, Object>>) athlete.getOrDefault("shoes", List.of());
            for (Map<String, Object> b : bikes) {
                options.add(new ResourceOption(str(b.get("id")), str(b.get("name")), "Bike"));
            }
            for (Map<String, Object> s : shoes) {
                options.add(new ResourceOption(str(s.get("id")), str(s.get("name")), "Shoes"));
            }
            return options;
        } catch (Exception e) {
            logger.error("[strava] Failed to list gear: {}", e.getMessage());
            return List.of();
        }
    }

    private String getToken(Map<String, Object> credentials) {
        if (credentials != null) {
            for (String key : new String[]{"accessToken", "token"}) {
                Object val = credentials.get(key);
                if (val != null && !val.toString().isBlank()) return val.toString();
            }
        }
        throw new IllegalArgumentException("Strava connection requires an OAuth2 access token.");
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
