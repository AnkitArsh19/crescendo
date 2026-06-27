package com.crescendo.apps.googletasks;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Google Tasks operations.
 *
 * <p>Operations (mirrors n8n {@code TaskDescription.ts} in Google/Task/):
 * <ul>
 *   <li>{@code create}   — tasks.insert</li>
 *   <li>{@code update}   — tasks.patch</li>
 *   <li>{@code delete}   — tasks.delete</li>
 *   <li>{@code complete} — tasks.patch with status=completed</li>
 *   <li>{@code getAll}   — tasks.list</li>
 * </ul>
 *
 * <p>Credentials: {@code accessToken} (OAuth2, tasks scope)
 */
@Component
public class GoogleTasksHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTasksHandlers.class);

    private static final String BASE = "https://tasks.googleapis.com/tasks/v1/lists";

    private final RestClient restClient;

    public GoogleTasksHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    /**
     * Create a new task in a task list.
     * Config: title (required), taskListId (default "@default"),
     *         notes, due (RFC 3339 timestamp), status (needsAction|completed),
     *         parent (task ID — creates sub-task), previous (task ID — inserts after)
     */
    @ActionMapping(appKey = "googletasks", actionKey = "create")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String title = require(config, "title");
        if (title == null) return ActionResult.failure("'title' is required");
        String taskListId = opt(config, "taskListId", "@default");

        logger.info("[googletasks] create: title='{}', list='{}'", title, taskListId);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("title", title);
            if (config.containsKey("notes")) body.put("notes", config.get("notes"));
            if (config.containsKey("due")) body.put("due", config.get("due"));
            if (config.containsKey("status")) body.put("status", config.get("status"));

            StringBuilder uri = new StringBuilder(BASE + "/" + encode(taskListId) + "/tasks");
            String parent = opt(config, "parent", null);
            String previous = opt(config, "previous", null);
            if (parent != null) uri.append("?parent=").append(encode(parent));
            if (previous != null) uri.append(parent != null ? "&" : "?").append("previous=").append(encode(previous));

            Map<String, Object> response = restClient.post()
                    .uri(uri.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googletasks] create failed: {}", e.getMessage());
            return ActionResult.failure("Google Tasks create failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    /**
     * Update an existing task (PATCH — only provided fields changed).
     * Config: taskId (required), taskListId (required), title, notes, due, status, completed, deleted
     */
    @ActionMapping(appKey = "googletasks", actionKey = "update")
    @SuppressWarnings("unchecked")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String taskListId = require(config, "taskListId");
        if (taskListId == null) return ActionResult.failure("'taskListId' is required");
        String taskId = require(config, "taskId");
        if (taskId == null) return ActionResult.failure("'taskId' is required");

        logger.info("[googletasks] update: taskId='{}', list='{}'", taskId, taskListId);

        try {
            Map<String, Object> patch = new HashMap<>();
            if (config.containsKey("title")) patch.put("title", config.get("title"));
            if (config.containsKey("notes")) patch.put("notes", config.get("notes"));
            if (config.containsKey("due")) patch.put("due", config.get("due"));
            if (config.containsKey("completed")) patch.put("completed", config.get("completed"));
            if (config.containsKey("deleted")) patch.put("deleted", Boolean.parseBoolean(config.get("deleted").toString()));
            if (config.containsKey("status")) patch.put("status", config.get("status"));

            Map<String, Object> response = restClient.patch()
                    .uri(BASE + "/" + encode(taskListId) + "/tasks/" + encode(taskId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googletasks] update failed: {}", e.getMessage());
            return ActionResult.failure("Google Tasks update failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    /**
     * Delete a task.
     * Config: taskId (required), taskListId (required)
     */
    @ActionMapping(appKey = "googletasks", actionKey = "delete")
    public ActionResult delete(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String taskListId = require(config, "taskListId");
        if (taskListId == null) return ActionResult.failure("'taskListId' is required");
        String taskId = require(config, "taskId");
        if (taskId == null) return ActionResult.failure("'taskId' is required");

        logger.info("[googletasks] delete: taskId='{}', list='{}'", taskId, taskListId);

        try {
            restClient.delete()
                    .uri(BASE + "/" + encode(taskListId) + "/tasks/" + encode(taskId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            return ActionResult.success(Map.of("deleted", true, "id", taskId));
        } catch (Exception e) {
            logger.error("[googletasks] delete failed: {}", e.getMessage());
            return ActionResult.failure("Google Tasks delete failed: " + e.getMessage());
        }
    }

    // ── complete ──────────────────────────────────────────────────────────────

    /**
     * Mark a task as completed.
     * Config: taskId (required), taskListId (required)
     */
    @ActionMapping(appKey = "googletasks", actionKey = "complete")
    @SuppressWarnings("unchecked")
    public ActionResult complete(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String taskListId = require(config, "taskListId");
        if (taskListId == null) return ActionResult.failure("'taskListId' is required");
        String taskId = require(config, "taskId");
        if (taskId == null) return ActionResult.failure("'taskId' is required");

        logger.info("[googletasks] complete: taskId='{}', list='{}'", taskId, taskListId);

        try {
            Map<String, Object> response = restClient.patch()
                    .uri(BASE + "/" + encode(taskListId) + "/tasks/" + encode(taskId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("status", "completed"))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googletasks] complete failed: {}", e.getMessage());
            return ActionResult.failure("Google Tasks complete failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    /**
     * List tasks in a task list.
     * Config: taskListId (required), maxResults (int, default 100),
     *         showCompleted (bool, default true), showHidden (bool, default false),
     *         showDeleted (bool, default false), dueMin, dueMax (RFC 3339 timestamps)
     */
    @ActionMapping(appKey = "googletasks", actionKey = "getAll")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String taskListId = require(config, "taskListId");
        if (taskListId == null) return ActionResult.failure("'taskListId' is required");

        boolean returnAll = Boolean.parseBoolean(opt(config, "returnAll", "false"));
        int maxResults = parseIntOpt(config, "maxResults", 100);
        boolean showCompleted = Boolean.parseBoolean(opt(config, "showCompleted", "true"));
        boolean showHidden = Boolean.parseBoolean(opt(config, "showHidden", "false"));
        boolean showDeleted = Boolean.parseBoolean(opt(config, "showDeleted", "false"));
        String dueMin = opt(config, "dueMin", null);
        String dueMax = opt(config, "dueMax", null);

        logger.info("[googletasks] getAll: list='{}', maxResults={}", taskListId, maxResults);

        try {
            StringBuilder uri = new StringBuilder(BASE + "/" + encode(taskListId) + "/tasks?");
            uri.append("maxResults=").append(returnAll ? 100 : maxResults);
            uri.append("&showCompleted=").append(showCompleted);
            uri.append("&showHidden=").append(showHidden);
            uri.append("&showDeleted=").append(showDeleted);
            if (dueMin != null) uri.append("&dueMin=").append(encode(dueMin));
            if (dueMax != null) uri.append("&dueMax=").append(encode(dueMax));

            Map<String, Object> response = restClient.get()
                    .uri(uri.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googletasks] getAll failed: {}", e.getMessage());
            return ActionResult.failure("Google Tasks getAll failed: " + e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String resolveToken(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        return token != null && !token.toString().isBlank() ? token.toString() : null;
    }

    private ActionResult missingToken() {
        return ActionResult.failure("Google Tasks requires an OAuth2 accessToken in connection credentials");
    }

    private String require(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : null;
    }

    private String opt(Map<String, Object> config, String key, String defaultVal) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : defaultVal;
    }

    private int parseIntOpt(Map<String, Object> config, String key, int defaultVal) {
        Object v = config.get(key);
        if (v == null) return defaultVal;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return defaultVal; }
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
