package com.crescendo.apps.microsoftteams;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Microsoft Teams Task operations (via To Do API).
 */
@Component
public class MicrosoftTeamsTaskHandlers {

    private static final String GRAPH_API = MicrosoftTeamsSupport.GRAPH_API;

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "createTask")
// @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String listId = MicrosoftTeamsSupport.require(config, "listId");
        String title = MicrosoftTeamsSupport.require(config, "title");

        if (listId == null || title == null) {
            return ActionResult.failure("'listId' and 'title' are required");
        }

        try {
            Map<String, Object> task = new HashMap<>();
            task.put("title", title);

            String response = MicrosoftTeamsSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/me/todo/lists/" + listId + "/tasks")
                    .body(task)
                    .retrieve()
                    .body(String.class);
            
            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            return ActionResult.success(output);
        } catch (Exception e) {
            return ActionResult.failure("Teams createTask failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "deleteTask")
    public ActionResult delete(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String listId = MicrosoftTeamsSupport.require(config, "listId");
        String taskId = MicrosoftTeamsSupport.require(config, "taskId");

        if (listId == null || taskId == null) {
            return ActionResult.failure("'listId' and 'taskId' are required");
        }

        try {
            MicrosoftTeamsSupport.clientBuilder(context).build().delete()
                    .uri(GRAPH_API + "/me/todo/lists/" + listId + "/tasks/" + taskId)
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "taskId", taskId));
        } catch (Exception e) {
            return ActionResult.failure("Teams deleteTask failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "getTask")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String listId = MicrosoftTeamsSupport.require(config, "listId");
        String taskId = MicrosoftTeamsSupport.require(config, "taskId");

        if (listId == null || taskId == null) {
            return ActionResult.failure("'listId' and 'taskId' are required");
        }

        try {
            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/todo/lists/" + listId + "/tasks/" + taskId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams getTask failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "getAllTasks")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String listId = MicrosoftTeamsSupport.require(config, "listId");

        if (listId == null) {
            return ActionResult.failure("'listId' is required");
        }

        try {
            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/todo/lists/" + listId + "/tasks")
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams getAllTasks failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "updateTask")
    @SuppressWarnings("unchecked")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String listId = MicrosoftTeamsSupport.require(config, "listId");
        String taskId = MicrosoftTeamsSupport.require(config, "taskId");

        if (listId == null || taskId == null) {
            return ActionResult.failure("'listId' and 'taskId' are required");
        }

        try {
            Map<String, Object> patch = new HashMap<>();
            String title = MicrosoftTeamsSupport.opt(config, "title", null);
            if (title != null) patch.put("title", title);
            
            String status = MicrosoftTeamsSupport.opt(config, "status", null);
            if (status != null) patch.put("status", status);

            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().patch()
                    .uri(GRAPH_API + "/me/todo/lists/" + listId + "/tasks/" + taskId)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams updateTask failed: " + e.getMessage());
        }
    }
}
