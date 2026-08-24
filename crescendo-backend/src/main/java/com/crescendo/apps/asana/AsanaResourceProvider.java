package com.crescendo.apps.asana;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class AsanaResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(AsanaResourceProvider.class);
    private static final String BASE_URL = "https://app.asana.com/api/1.0";

    @Override
    public String appKey() {
        return "asana";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("workspaces", "projects", "users", "tasks");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String token = extractToken(credentials);
        if (token == null || token.isBlank()) {
            logger.warn("[asana] Missing accessToken for resource listing");
            return List.of();
        }

        return switch (resourceType) {
            case "workspaces" -> listWorkspaces(token);
            case "projects" -> listProjects(token, params != null ? params.get("workspaceId") : null);
            case "users" -> listUsers(token, params != null ? params.get("workspaceId") : null);
            case "tasks" -> listTasks(token, params != null ? params.get("projectId") : null);
            default -> List.of();
        };
    }

    private String extractToken(Map<String, Object> credentials) {
        if (credentials == null) return null;
        if (credentials.get("accessToken") != null) return credentials.get("accessToken").toString();
        if (credentials.get("apiKey") != null) return credentials.get("apiKey").toString();
        if (credentials.get("token") != null) return credentials.get("token").toString();
        return null;
    }

    private RestClient createClient(String token) {
        return RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listWorkspaces(String token) {
        try {
            Map<String, Object> resp = createClient(token).get()
                    .uri("/workspaces")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("data") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String gid = String.valueOf(item.get("gid"));
                        String name = String.valueOf(item.get("name"));
                        options.add(new ResourceOption(gid, name));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[asana] Error fetching workspaces: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listProjects(String token, String workspaceId) {
        try {
            String uri = (workspaceId != null && !workspaceId.isBlank())
                    ? "/workspaces/" + workspaceId + "/projects"
                    : "/projects";

            Map<String, Object> resp = createClient(token).get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("data") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String gid = String.valueOf(item.get("gid"));
                        String name = String.valueOf(item.get("name"));
                        options.add(new ResourceOption(gid, name));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[asana] Error fetching projects: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listUsers(String token, String workspaceId) {
        try {
            String uri = (workspaceId != null && !workspaceId.isBlank())
                    ? "/workspaces/" + workspaceId + "/users"
                    : "/users";

            Map<String, Object> resp = createClient(token).get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("data") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String gid = String.valueOf(item.get("gid"));
                        String name = String.valueOf(item.get("name"));
                        options.add(new ResourceOption(gid, name));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[asana] Error fetching users: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listTasks(String token, String projectId) {
        if (projectId == null || projectId.isBlank()) return List.of();
        try {
            Map<String, Object> resp = createClient(token).get()
                    .uri("/projects/" + projectId + "/tasks")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("data") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String gid = String.valueOf(item.get("gid"));
                        String name = String.valueOf(item.get("name"));
                        options.add(new ResourceOption(gid, name));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[asana] Error fetching tasks: {}", e.getMessage());
        }
        return List.of();
    }
}
