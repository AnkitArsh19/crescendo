package com.crescendo.apps.googletasks;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Deletes a Google Task via tasks.delete.
 */
@ActionMapping(appKey = "google-tasks", actionKey = "delete-task")
public class GoogleTasksDeleteTaskHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTasksDeleteTaskHandler.class);
    private static final String TASKS_API = "https://tasks.googleapis.com/tasks/v1/lists/";
    private final RestClient restClient = RestClient.create();

    @Override
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

        logger.info("[google-tasks] Deleting task '{}' from list '{}'", taskId, taskListId);

        try {
            String url = TASKS_API + taskListId + "/tasks/" + taskId;
            restClient.delete()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-tasks");
            output.put("action", "delete-task");
            output.put("taskId", taskId);
            output.put("deleted", true);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-tasks] Delete task failed: {}", e.getMessage());
            return ActionResult.failure("Google Tasks delete-task failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
