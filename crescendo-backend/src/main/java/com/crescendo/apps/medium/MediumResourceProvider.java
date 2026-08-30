package com.crescendo.apps.medium;

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
 * Fetches Medium resources: user profile and publications.
 * Authenticates via Integration Token.
 */
@Component
@SuppressWarnings("unchecked")
public class MediumResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(MediumResourceProvider.class);
    private static final String MEDIUM_API = "https://api.medium.com/v1";

    private final RestClient restClient;

    public MediumResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "medium";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("publications", "me");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("publications", 50, java.time.Duration.ofMinutes(10)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String token = getToken(credentials);
        return switch (resourceType) {
            case "publications" -> listPublications(token);
            case "me"           -> listMe(token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listMe(String token) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(MEDIUM_API + "/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (resp == null || !resp.containsKey("data")) return List.of();
            Map<String, Object> data = (Map<String, Object>) resp.get("data");
            if (data == null) return List.of();

            String id = str(data.get("id"));
            String username = str(data.get("username"));
            String name = str(data.get("name"));
            return List.of(new ResourceOption(id, name + " (@" + username + ")", "Personal Profile"));
        } catch (Exception e) {
            logger.error("[medium] Failed to fetch user profile: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listPublications(String token) {
        try {
            // First get user ID
            Map<String, Object> meResp = restClient.get()
                    .uri(MEDIUM_API + "/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (meResp == null || !meResp.containsKey("data")) return List.of();
            Map<String, Object> data = (Map<String, Object>) meResp.get("data");
            if (data == null) return List.of();
            String userId = str(data.get("id"));

            Map<String, Object> pubResp = restClient.get()
                    .uri(MEDIUM_API + "/users/" + userId + "/publications")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (pubResp == null || !pubResp.containsKey("data")) return List.of();
            List<Map<String, Object>> pubs = (List<Map<String, Object>>) pubResp.get("data");
            if (pubs == null) return List.of();

            return pubs.stream().map(p -> {
                String id = str(p.get("id"));
                String name = str(p.get("name"));
                String desc = str(p.get("description"));
                return new ResourceOption(id, name.isBlank() ? "Publication " + id : name, desc);
            }).toList();
        } catch (Exception e) {
            logger.error("[medium] Failed to list publications: {}", e.getMessage());
            return List.of();
        }
    }

    private String getToken(Map<String, Object> credentials) {
        if (credentials != null) {
            for (String key : new String[]{"integrationToken", "accessToken", "token", "apiKey"}) {
                Object val = credentials.get(key);
                if (val != null && !val.toString().isBlank()) return val.toString();
            }
        }
        throw new IllegalArgumentException("Medium requires an integrationToken.");
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
