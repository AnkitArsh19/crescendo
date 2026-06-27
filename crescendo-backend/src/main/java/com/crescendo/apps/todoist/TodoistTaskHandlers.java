package com.crescendo.apps.todoist;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Todoist Task handlers.
 */
@Component
public class TodoistTaskHandlers {

    private String getBaseUrl() {
        return "https://api.todoist.com/rest/v2";
    }

    private String getSyncBaseUrl() {
        return "https://api.todoist.com/sync/v9";
    }

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("apiKey");
    }

    // ─── TASK ───

    @ActionMapping(appKey = "todoist", actionKey = "todoist:task:create")
    public Object createTask(ActionContext context) throws Exception {
        String content = context.getString("content");
        String projectId = context.getString("projectId");
        String sectionId = context.getString("sectionId");
        String description = context.getString("description");

        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        if (projectId != null) body.put("project_id", projectId);
        if (sectionId != null) body.put("section_id", sectionId);
        if (description != null) body.put("description", description);

        return RestClient.builder()
                .url(getBaseUrl() + "/tasks")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:task:quickAdd")
    public Object quickAddTask(ActionContext context) throws Exception {
        String text = context.getString("text");

        return RestClient.builder()
                .url(getSyncBaseUrl() + "/quick/add")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("text", text))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:task:get")
    public Object getTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(getBaseUrl() + "/tasks/" + taskId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:task:getAll")
    public Object getAllTasks(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String url = getBaseUrl() + "/tasks";
        if (projectId != null && !projectId.isBlank()) {
            url += "?project_id=" + projectId;
        }

        return RestClient.builder()
                .url(url)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:task:update")
    public Object updateTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String content = context.getString("content");

        Map<String, Object> body = new HashMap<>();
        if (content != null) body.put("content", content);

        return RestClient.builder()
                .url(getBaseUrl() + "/tasks/" + taskId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:task:delete")
    public Object deleteTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(getBaseUrl() + "/tasks/" + taskId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:task:close")
    public Object closeTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(getBaseUrl() + "/tasks/" + taskId + "/close")
                .header("Authorization", getAuth(context))
                .post(Map.of())
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:task:reopen")
    public Object reopenTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(getBaseUrl() + "/tasks/" + taskId + "/reopen")
                .header("Authorization", getAuth(context))
                .post(Map.of())
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:task:move")
    public Object moveTask(ActionContext context) throws Exception {
        // Requires Sync API
        String taskId = context.getString("taskId");
        String projectId = context.getString("projectId");

        Map<String, Object> command = new HashMap<>();
        command.put("type", "item_move");
        command.put("uuid", java.util.UUID.randomUUID().toString());
        command.put("args", Map.of("id", taskId, "project_id", projectId));

        return RestClient.builder()
                .url(getSyncBaseUrl() + "/sync")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("commands", java.util.List.of(command)))
                .execute();
    }
}
