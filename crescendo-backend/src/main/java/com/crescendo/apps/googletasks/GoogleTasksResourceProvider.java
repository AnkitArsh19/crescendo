package com.crescendo.apps.googletasks;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Google Tasks resources for dynamic dropdowns.
 * Supports: taskLists, tasks (depends on taskListId)
 */
@Component
public class GoogleTasksResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTasksResourceProvider.class);
    private static final String TASKS_API = "https://tasks.googleapis.com/tasks/v1";

    @Override
    public String appKey() {
        return "google-tasks";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("taskLists", "tasks");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = credentials.get("accessToken").toString();

        return switch (resourceType) {
            case "taskLists" -> listTaskLists(accessToken);
            case "tasks" -> listTasks(accessToken, params.get("taskListId"));
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listTaskLists(String accessToken) {
        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(TASKS_API + "/users/@me/lists?maxResults=100")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null) return List.of();

            return items.stream()
                    .map(list -> new ResourceOption(
                            list.get("id").toString(),
                            list.get("title").toString(),
                            null
                    ))
                    .toList();
        } catch (Exception e) {
            logger.error("[google-tasks] Failed to list task lists: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listTasks(String accessToken, String taskListId) {
        if (taskListId == null || taskListId.isBlank()) return List.of();

        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(TASKS_API + "/lists/{taskListId}/tasks?maxResults=100&showCompleted=true", taskListId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null) return List.of();

            return items.stream()
                    .map(task -> new ResourceOption(
                            task.get("id").toString(),
                            task.get("title") != null ? task.get("title").toString() : "(No title)",
                            task.get("status") != null ? task.get("status").toString() : null
                    ))
                    .toList();
        } catch (Exception e) {
            logger.error("[google-tasks] Failed to list tasks: {}", e.getMessage());
            return List.of();
        }
    }

    private RestClient restClient(String accessToken) {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
