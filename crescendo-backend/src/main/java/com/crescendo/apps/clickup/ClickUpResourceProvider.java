package com.crescendo.apps.clickup;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class ClickUpResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(ClickUpResourceProvider.class);
    private static final String BASE_URL = "https://api.clickup.com/api/v2";

    @Override
    public String appKey() {
        return "clickup";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("teams", "spaces", "folders", "lists", "tasks");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String token = extractToken(credentials);
        if (token == null || token.isBlank()) {
            logger.warn("[clickup] Missing API token for resource listing");
            return List.of();
        }

        return switch (resourceType) {
            case "teams" -> listTeams(token);
            case "spaces" -> listSpaces(token, params != null ? params.get("teamId") : null);
            case "folders" -> listFolders(token, params != null ? params.get("spaceId") : null);
            case "lists" -> listLists(token, params != null ? params.get("folderId") : null, params != null ? params.get("spaceId") : null);
            case "tasks" -> listTasks(token, params != null ? params.get("listId") : null);
            default -> List.of();
        };
    }

    private String extractToken(Map<String, Object> credentials) {
        if (credentials == null) return null;
        if (credentials.get("apiKey") != null) return credentials.get("apiKey").toString();
        if (credentials.get("apiToken") != null) return credentials.get("apiToken").toString();
        if (credentials.get("accessToken") != null) return credentials.get("accessToken").toString();
        if (credentials.get("token") != null) return credentials.get("token").toString();
        return null;
    }

    private RestClient createClient(String token) {
        return RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, token.startsWith("pk_") ? token : "Bearer " + token)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listTeams(String token) {
        try {
            Map<String, Object> resp = createClient(token).get()
                    .uri("/team")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("teams") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        String name = String.valueOf(item.get("name"));
                        options.add(new ResourceOption(id, name));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[clickup] Error fetching teams: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listSpaces(String token, String teamId) {
        if (teamId == null || teamId.isBlank()) return List.of();
        try {
            Map<String, Object> resp = createClient(token).get()
                    .uri("/team/" + teamId + "/space")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("spaces") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        String name = String.valueOf(item.get("name"));
                        options.add(new ResourceOption(id, name));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[clickup] Error fetching spaces: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listFolders(String token, String spaceId) {
        if (spaceId == null || spaceId.isBlank()) return List.of();
        try {
            Map<String, Object> resp = createClient(token).get()
                    .uri("/space/" + spaceId + "/folder")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("folders") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        String name = String.valueOf(item.get("name"));
                        options.add(new ResourceOption(id, name));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[clickup] Error fetching folders: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listLists(String token, String folderId, String spaceId) {
        try {
            String uri = (folderId != null && !folderId.isBlank())
                    ? "/folder/" + folderId + "/list"
                    : (spaceId != null && !spaceId.isBlank()) ? "/space/" + spaceId + "/list" : null;

            if (uri == null) return List.of();

            Map<String, Object> resp = createClient(token).get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("lists") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        String name = String.valueOf(item.get("name"));
                        options.add(new ResourceOption(id, name));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[clickup] Error fetching lists: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listTasks(String token, String listId) {
        if (listId == null || listId.isBlank()) return List.of();
        try {
            Map<String, Object> resp = createClient(token).get()
                    .uri("/list/" + listId + "/task")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("tasks") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        String name = String.valueOf(item.get("name"));
                        options.add(new ResourceOption(id, name));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[clickup] Error fetching tasks: {}", e.getMessage());
        }
        return List.of();
    }
}
