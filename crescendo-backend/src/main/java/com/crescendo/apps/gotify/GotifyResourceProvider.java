package com.crescendo.apps.gotify;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Gotify resources: applications and clients.
 * Authenticates via App Token or Client Token.
 */
@Component
@SuppressWarnings("unchecked")
public class GotifyResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GotifyResourceProvider.class);

    private final RestClient restClient;

    public GotifyResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "gotify";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("applications", "clients");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("applications", 50, java.time.Duration.ofMinutes(10)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String baseUrl = str(credentials.get("baseUrl")).replaceAll("/+$", "");
        String token = getToken(credentials);
        if (baseUrl.isBlank() || token.isBlank()) {
            throw new IllegalArgumentException("Gotify requires baseUrl and appToken.");
        }

        return switch (resourceType) {
            case "applications" -> listApplications(baseUrl, token);
            case "clients"      -> listClients(baseUrl, token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listApplications(String baseUrl, String token) {
        try {
            List<Map<String, Object>> apps = restClient.get()
                    .uri(baseUrl + "/application")
                    .header("X-Gotify-Key", token)
                    .retrieve().body(List.class);
            if (apps == null) return List.of();
            return apps.stream().map(a -> {
                String id = String.valueOf(a.get("id"));
                String name = str(a.get("name"));
                String desc = str(a.get("description"));
                return new ResourceOption(id, name.isBlank() ? "App " + id : name, desc);
            }).toList();
        } catch (Exception e) {
            logger.error("[gotify] Failed to list applications: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listClients(String baseUrl, String token) {
        try {
            List<Map<String, Object>> clients = restClient.get()
                    .uri(baseUrl + "/client")
                    .header("X-Gotify-Key", token)
                    .retrieve().body(List.class);
            if (clients == null) return List.of();
            return clients.stream().map(c -> {
                String id = String.valueOf(c.get("id"));
                String name = str(c.get("name"));
                return new ResourceOption(id, name.isBlank() ? "Client " + id : name, "Gotify Client");
            }).toList();
        } catch (Exception e) {
            logger.error("[gotify] Failed to list clients: {}", e.getMessage());
            return List.of();
        }
    }

    private String getToken(Map<String, Object> credentials) {
        if (credentials != null) {
            for (String key : new String[]{"appToken", "clientToken", "accessToken", "token", "apiKey"}) {
                Object val = credentials.get(key);
                if (val != null && !val.toString().isBlank()) return val.toString();
            }
        }
        return "";
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
