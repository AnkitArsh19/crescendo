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
 * Updates a Google Task via tasks.patch (title, notes, due, status).
 */
@ActionMapping(appKey = "google-tasks", actionKey = "update-task")
public class GoogleTasksUpdateTaskHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTasksUpdateTaskHandler.class);
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

        logger.info("[google-tasks] Updating task '{}' in list '{}'", taskId, taskListId);

        try {
            Map<String, Object> patch = new HashMap<>();
            if (config.containsKey("title")) patch.put("title", config.get("title"));
            if (config.containsKey("notes")) patch.put("notes", config.get("notes"));
            if (config.containsKey("due")) patch.put("due", config.get("due"));
            if (config.containsKey("status")) patch.put("status", config.get("status"));

            String url = TASKS_API + taskListId + "/tasks/" + taskId;
            Map<String, Object> response = restClient.patch()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-tasks");
            output.put("action", "update-task");
            output.put("taskId", taskId);
            output.put("title", response != null ? response.get("title") : null);
            output.put("status", response != null ? response.get("status") : null);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-tasks] Update task failed: {}", e.getMessage());
            return ActionResult.failure("Google Tasks update-task failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
