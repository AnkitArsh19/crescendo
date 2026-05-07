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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "google-tasks", actionKey = "create-task")
public class GoogleTasksCreateTaskHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTasksCreateTaskHandler.class);
    private static final String TASKS_API = "https://tasks.googleapis.com/tasks/v1/lists";

    private final RestClient restClient;

    public GoogleTasksCreateTaskHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = asString(creds != null ? creds.get("accessToken") : null);
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Tasks requires an 'accessToken' in connection credentials");
        }

        String title = asString(config.get("title"));
        if (title == null || title.isBlank()) {
            return ActionResult.failure("'title' is required");
        }

        String taskListId = defaultIfBlank(asString(config.get("taskListId")), "@default");

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        if (config.get("notes") != null) {
            body.put("notes", config.get("notes"));
        }
        if (config.get("due") != null) {
            body.put("due", config.get("due"));
        }

        try {
            String response = restClient.post()
                    .uri(TASKS_API + "/" + URLEncoder.encode(taskListId, StandardCharsets.UTF_8) + "/tasks")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-tasks");
            output.put("taskListId", taskListId);
            output.put("response", response);
            logger.info("[google-tasks] Task created successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-tasks] Failed to create task", e);
            return ActionResult.failure("Google Tasks create task failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}