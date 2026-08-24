package com.crescendo.apps.todoist;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class TodoistResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(TodoistResourceProvider.class);
    private static final String BASE_URL = "https://api.todoist.com/rest/v2";

    @Override
    public String appKey() {
        return "todoist";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("projects", "sections", "labels", "tasks");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String token = extractToken(credentials);
        if (token == null || token.isBlank()) {
            logger.warn("[todoist] Missing API token for resource listing");
            return List.of();
        }

        return switch (resourceType) {
            case "projects" -> listProjects(token);
            case "sections" -> listSections(token, params != null ? params.get("projectId") : null);
            case "labels" -> listLabels(token);
            case "tasks" -> listTasks(token, params != null ? params.get("projectId") : null);
            default -> List.of();
        };
    }

    private String extractToken(Map<String, Object> credentials) {
        if (credentials == null) return null;
        if (credentials.get("apiKey") != null) return credentials.get("apiKey").toString();
        if (credentials.get("accessToken") != null) return credentials.get("accessToken").toString();
        if (credentials.get("token") != null) return credentials.get("token").toString();
        return null;
    }

    private RestClient createClient(String token) {
        return RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    private List<ResourceOption> listProjects(String token) {
        try {
            List<Map<String, Object>> projects = createClient(token).get()
                    .uri("/projects")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (projects == null) return List.of();
            List<ResourceOption> options = new ArrayList<>();
            for (Map<String, Object> p : projects) {
                String id = String.valueOf(p.get("id"));
                String name = String.valueOf(p.get("name"));
                options.add(new ResourceOption(id, name));
            }
            return options;
        } catch (Exception e) {
            logger.warn("[todoist] Error fetching projects: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listSections(String token, String projectId) {
        try {
            String uri = (projectId != null && !projectId.isBlank())
                    ? "/sections?project_id=" + projectId
                    : "/sections";

            List<Map<String, Object>> sections = createClient(token).get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (sections == null) return List.of();
            List<ResourceOption> options = new ArrayList<>();
            for (Map<String, Object> s : sections) {
                String id = String.valueOf(s.get("id"));
                String name = String.valueOf(s.get("name"));
                options.add(new ResourceOption(id, name));
            }
            return options;
        } catch (Exception e) {
            logger.warn("[todoist] Error fetching sections: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listLabels(String token) {
        try {
            List<Map<String, Object>> labels = createClient(token).get()
                    .uri("/labels")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (labels == null) return List.of();
            List<ResourceOption> options = new ArrayList<>();
            for (Map<String, Object> l : labels) {
                String id = String.valueOf(l.get("id"));
                String name = String.valueOf(l.get("name"));
                options.add(new ResourceOption(id, name));
            }
            return options;
        } catch (Exception e) {
            logger.warn("[todoist] Error fetching labels: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listTasks(String token, String projectId) {
        try {
            String uri = (projectId != null && !projectId.isBlank())
                    ? "/tasks?project_id=" + projectId
                    : "/tasks";

            List<Map<String, Object>> tasks = createClient(token).get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (tasks == null) return List.of();
            List<ResourceOption> options = new ArrayList<>();
            for (Map<String, Object> t : tasks) {
                String id = String.valueOf(t.get("id"));
                String content = String.valueOf(t.get("content"));
                options.add(new ResourceOption(id, content));
            }
            return options;
        } catch (Exception e) {
            logger.warn("[todoist] Error fetching tasks: {}", e.getMessage());
            return List.of();
        }
    }
}
