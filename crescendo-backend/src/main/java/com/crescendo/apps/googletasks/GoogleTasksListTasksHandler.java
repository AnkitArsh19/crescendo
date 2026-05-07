package com.crescendo.apps.googletasks;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "google-tasks", actionKey = "list-tasks")
public class GoogleTasksListTasksHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTasksListTasksHandler.class);
    private static final String TASKS_API = "https://tasks.googleapis.com/tasks/v1/lists";

    private final RestClient restClient;

    public GoogleTasksListTasksHandler() {
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

        String taskListId = defaultIfBlank(asString(config.get("taskListId")), "@default");
        int maxResults = parseInt(config.get("maxResults"), 20);
        boolean showCompleted = parseBoolean(config.get("showCompleted"), true);

        try {
            String uri = TASKS_API + "/" + URLEncoder.encode(taskListId, StandardCharsets.UTF_8)
                    + "/tasks?maxResults=" + maxResults + "&showCompleted=" + showCompleted;

            String response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-tasks");
            output.put("taskListId", taskListId);
            output.put("response", response);
            logger.info("[google-tasks] Tasks listed successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-tasks] Failed to list tasks", e);
            return ActionResult.failure("Google Tasks list tasks failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value.toString());
    }
}