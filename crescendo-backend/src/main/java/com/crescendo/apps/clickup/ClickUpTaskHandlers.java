package com.crescendo.apps.clickup;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * ClickUp Task handlers.
 */
@Component
public class ClickUpTaskHandlers {

    private String getBaseUrl() {
        return "https://api.clickup.com/api/v2";
    }

    private String getAuth(ActionContext context) {
        return context.getCredential("apiToken");
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:task:create")
    public Object createTask(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(getBaseUrl() + "/list/" + listId + "/task")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:task:delete")
    public Object deleteTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(getBaseUrl() + "/task/" + taskId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:task:get")
    public Object getTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(getBaseUrl() + "/task/" + taskId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:task:getAll")
    public Object getAllTasks(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        return RestClient.builder()
                .url(getBaseUrl() + "/list/" + listId + "/task")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:task:update")
    public Object updateTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String name = context.getString("name");

        Map<String, Object> body = new HashMap<>();
        if (name != null) body.put("name", name);

        return RestClient.builder()
                .url(getBaseUrl() + "/task/" + taskId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }

    // Dependencies
    @ActionMapping(appKey = "clickup", actionKey = "clickup:taskDependency:create")
    public Object createTaskDependency(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String dependsOnTaskId = context.getString("dependsOnTaskId");

        return RestClient.builder()
                .url(getBaseUrl() + "/task/" + taskId + "/dependency")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("depends_on", dependsOnTaskId))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:taskDependency:delete")
    public Object deleteTaskDependency(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String dependsOnTaskId = context.getString("dependsOnTaskId");

        return RestClient.builder()
                .url(getBaseUrl() + "/task/" + taskId + "/dependency?depends_on=" + dependsOnTaskId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    // Removing Tags/Lists
    @ActionMapping(appKey = "clickup", actionKey = "clickup:taskList:remove")
    public Object removeTaskList(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String listId = context.getString("listId");
        return RestClient.builder()
                .url(getBaseUrl() + "/list/" + listId + "/task/" + taskId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:taskTag:remove")
    public Object removeTaskTag(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String tagName = context.getString("tagName");
        return RestClient.builder()
                .url(getBaseUrl() + "/task/" + taskId + "/tag/" + tagName)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
