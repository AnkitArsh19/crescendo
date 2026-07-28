package com.crescendo.apps.toggl;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Supplies the workspace and project selectors used by the Toggl catalog. */
@Component
public class TogglResourceProvider implements ResourceProvider {

    private static final String TOGGL_API = "https://api.track.toggl.com/api/v9";

    @Override
    public String appKey() {
        return "toggl";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("workspaces", "projects");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
                                                Map<String, String> params) {
        String apiToken = credential(credentials, "apiToken", "apiKey");
        if (apiToken == null) return List.of();

        try {
            RestClient client = RestClient.builder()
                    .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(apiToken))
                    .build();
            return switch (resourceType) {
                case "workspaces" -> {
                    List<Map<String, Object>> workspaces = client.get().uri(TOGGL_API + "/me/workspaces")
                            .retrieve().body(List.class);
                    yield workspaces == null ? List.of() : workspaces.stream()
                            .map(workspace -> new ResourceOption(String.valueOf(workspace.get("id")),
                                    String.valueOf(workspace.get("name")), "Toggl workspace"))
                            .toList();
                }
                case "projects" -> {
                    String workspaceId = params.get("workspaceId");
                    if (workspaceId == null || workspaceId.isBlank()) yield List.of();
                    List<Map<String, Object>> projects = client.get()
                            .uri(TOGGL_API + "/workspaces/{workspaceId}/projects", workspaceId)
                            .retrieve().body(List.class);
                    yield projects == null ? List.of() : projects.stream()
                            .map(project -> new ResourceOption(String.valueOf(project.get("id")),
                                    String.valueOf(project.get("name")), "Toggl project"))
                            .toList();
                }
                default -> List.of();
            };
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static String credential(Map<String, Object> credentials, String... names) {
        for (String name : names) {
            Object value = credentials.get(name);
            if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value);
        }
        return null;
    }

    private static String basicAuth(String token) {
        String pair = token + ":api_token";
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }
}
