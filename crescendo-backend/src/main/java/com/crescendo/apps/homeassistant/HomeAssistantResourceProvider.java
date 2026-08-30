package com.crescendo.apps.homeassistant;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fetches Home Assistant resources: entities grouped by domain and services.
 * Authenticates via Long-Lived Access Token.
 */
@Component
@SuppressWarnings("unchecked")
public class HomeAssistantResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(HomeAssistantResourceProvider.class);

    private final RestClient restClient;

    public HomeAssistantResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "home-assistant";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("entities", "lights", "switches", "sensors", "media_players", "climate", "scenes", "automations", "services");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("entities", 200, java.time.Duration.ofMinutes(3)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String baseUrl = str(credentials.get("baseUrl")).replaceAll("/+$", "");
        String token = str(credentials.get("accessToken"));
        if (baseUrl.isBlank() || token.isBlank()) {
            throw new IllegalArgumentException("Home Assistant requires baseUrl and accessToken credentials.");
        }

        return switch (resourceType) {
            case "entities"      -> listEntities(baseUrl, token, null);
            case "lights"        -> listEntities(baseUrl, token, "light");
            case "switches"      -> listEntities(baseUrl, token, "switch");
            case "sensors"       -> listEntities(baseUrl, token, "sensor");
            case "media_players" -> listEntities(baseUrl, token, "media_player");
            case "climate"       -> listEntities(baseUrl, token, "climate");
            case "scenes"        -> listEntities(baseUrl, token, "scene");
            case "automations"   -> listEntities(baseUrl, token, "automation");
            case "services"      -> listServices(baseUrl, token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listEntities(String baseUrl, String token, String domainFilter) {
        try {
            List<Map<String, Object>> states = restClient.get()
                    .uri(baseUrl + "/api/states")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(List.class);
            if (states == null) return List.of();
            return states.stream()
                    .filter(s -> {
                        String entityId = str(s.get("entity_id"));
                        return domainFilter == null || entityId.startsWith(domainFilter + ".");
                    })
                    .map(s -> {
                        String entityId = str(s.get("entity_id"));
                        Map<String, Object> attrs = (Map<String, Object>) s.getOrDefault("attributes", Map.of());
                        String friendlyName = str(attrs.get("friendly_name"));
                        String state = str(s.get("state"));
                        String label = friendlyName.isBlank() ? entityId : friendlyName;
                        return new ResourceOption(entityId, label, "State: " + state);
                    })
                    .sorted(Comparator.comparing(ResourceOption::label))
                    .toList();
        } catch (Exception e) {
            logger.error("[home-assistant] Failed to list entities (domain={}): {}", domainFilter, e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listServices(String baseUrl, String token) {
        try {
            List<Map<String, Object>> services = restClient.get()
                    .uri(baseUrl + "/api/services")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(List.class);
            if (services == null) return List.of();
            List<ResourceOption> options = new ArrayList<>();
            for (Map<String, Object> domainObj : services) {
                String domain = str(domainObj.get("domain"));
                Map<String, Object> svcs = (Map<String, Object>) domainObj.getOrDefault("services", Map.of());
                for (String svcName : svcs.keySet()) {
                    Map<String, Object> svcDef = (Map<String, Object>) svcs.get(svcName);
                    String description = str(svcDef.get("description"));
                    options.add(new ResourceOption(domain + "." + svcName,
                            domain + "." + svcName,
                            description.isBlank() ? domain : description));
                }
            }
            options.sort(Comparator.comparing(ResourceOption::label, String.CASE_INSENSITIVE_ORDER));
            return options;
        } catch (Exception e) {
            logger.error("[home-assistant] Failed to list services: {}", e.getMessage());
            return List.of();
        }
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
