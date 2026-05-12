package com.crescendo.apps.googletasks;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Marks a Google Task as completed via tasks.patch with status=completed.
 */
@ActionMapping(appKey = "google-tasks", actionKey = "complete-task")
public class GoogleTasksCompleteTaskHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTasksCompleteTaskHandler.class);
    private static final String TASKS_API = "https://tasks.googleapis.com/tasks/v1/lists/";
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Tasks requires an OAuth2 accessToken");
        }

        String taskListId = str(config, "taskListId");
        String taskId = str(config, "taskId");
        if (taskListId == null) return ActionResult.failure("'taskListId' is required");
        if (taskId == null) return ActionResult.failure("'taskId' is required");

        logger.info("[google-tasks] Completing task '{}' in list '{}'", taskId, taskListId);

        try {
            String url = TASKS_API + taskListId + "/tasks/" + taskId;
            Map<String, Object> response = restClient.patch()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", "completed"))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-tasks");
            output.put("action", "complete-task");
            output.put("taskId", taskId);
            output.put("status", "completed");
            output.put("title", response != null ? response.get("title") : null);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-tasks] Complete task failed: {}", e.getMessage());
            return ActionResult.failure("Google Tasks complete-task failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
