package com.crescendo.apps.asana;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Asana Task handlers.
 * Operations (from n8n Asana.node.ts, resource='task'):
 *   - create : POST /1.0/tasks
 *   - delete : DELETE /1.0/tasks/{taskId}
 *   - get    : GET  /1.0/tasks/{taskId}
 *   - getAll : GET  /1.0/tasks (filtered by project, workspace, assignee)
 *   - move   : POST /1.0/tasks/{taskId}/addProject (with sectionId)
 *   - search : GET  /1.0/workspaces/{workspaceId}/tasks/search
 *   - update : PUT  /1.0/tasks/{taskId}
 */
@Component
public class AsanaTaskHandlers {

    private static final String BASE = "https://app.asana.com/api/1.0";

    private String auth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:task:create")
    public Object createTask(ActionContext context) throws Exception {
        String name = context.getString("name");
        String workspaceId = context.getString("workspaceId");
        String projectId = context.getString("projectId");
        String dueOn = context.getString("dueOn");
        String assignee = context.getString("assignee");
        String notes = context.getString("notes");
        Map<String, Object> additional = context.getMap("additionalFields");

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        if (workspaceId != null) data.put("workspace", workspaceId);
        if (projectId != null) data.put("projects", java.util.List.of(projectId));
        if (dueOn != null) data.put("due_on", dueOn);
        if (assignee != null) data.put("assignee", assignee);
        if (notes != null) data.put("notes", notes);
        if (additional != null) data.putAll(additional);

        return RestClient.builder()
                .url(BASE + "/tasks")
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("data", data))
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:task:delete")
    public Object deleteTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(BASE + "/tasks/" + taskId)
                .header("Authorization", auth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:task:get")
    public Object getTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(BASE + "/tasks/" + taskId)
                .header("Authorization", auth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:task:getAll")
    public Object getAllTasks(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String workspaceId = context.getString("workspaceId");
        String assignee = context.getString("assignee");
        int limit = context.getInt("limit", 100);

        StringBuilder url = new StringBuilder(BASE + "/tasks?limit=" + limit);
        if (projectId != null && !projectId.isBlank()) url.append("&project=").append(projectId);
        else if (workspaceId != null && !workspaceId.isBlank()) url.append("&workspace=").append(workspaceId);
        if (assignee != null && !assignee.isBlank()) url.append("&assignee=").append(assignee);

        return RestClient.builder()
                .url(url.toString())
                .header("Authorization", auth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:task:move")
    public Object moveTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String projectId = context.getString("projectId");
        String sectionId = context.getString("sectionId");

        Map<String, Object> data = Map.of(
                "project", projectId,
                "section", sectionId
        );

        return RestClient.builder()
                .url(BASE + "/tasks/" + taskId + "/addProject")
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("data", data))
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:task:search")
    public Object searchTasks(ActionContext context) throws Exception {
        String workspaceId = context.getString("workspaceId");
        String text = context.getString("text");
        Map<String, Object> additional = context.getMap("additionalFields");

        StringBuilder url = new StringBuilder(BASE + "/workspaces/" + workspaceId + "/tasks/search");
        String sep = "?";
        if (text != null && !text.isBlank()) { url.append(sep).append("text=").append(java.net.URLEncoder.encode(text, "UTF-8")); sep = "&"; }
        if (additional != null) {
            for (Map.Entry<String, Object> e : additional.entrySet()) {
                url.append(sep).append(e.getKey()).append("=").append(e.getValue()); sep = "&";
            }
        }

        return RestClient.builder()
                .url(url.toString())
                .header("Authorization", auth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:task:update")
    public Object updateTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        Map<String, Object> fields = context.getMap("additionalFields");
        if (fields == null) fields = Map.of();

        return RestClient.builder()
                .url(BASE + "/tasks/" + taskId)
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .put(Map.of("data", fields))
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:taskProject:add")
    public Object addToProject(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String projectId = context.getString("projectId");
        return RestClient.builder()
                .url(BASE + "/tasks/" + taskId + "/addProject")
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("data", Map.of("project", projectId)))
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:taskProject:remove")
    public Object removeFromProject(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String projectId = context.getString("projectId");
        return RestClient.builder()
                .url(BASE + "/tasks/" + taskId + "/removeProject")
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("data", Map.of("project", projectId)))
                .execute();
    }
}
