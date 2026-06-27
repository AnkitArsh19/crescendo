package com.crescendo.apps.asana;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Asana Subtask handlers.
 * Operations (from n8n Asana.node.ts, resource='subtask'):
 *   - create : POST /1.0/tasks/{taskId}/subtasks
 *   - getAll : GET  /1.0/tasks/{taskId}/subtasks
 */
@Component
public class AsanaSubtaskHandlers {

    private static final String BASE = "https://app.asana.com/api/1.0";

    private String auth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:subtask:create")
    public Object createSubtask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String name = context.getString("name");
        Map<String, Object> additional = context.getMap("additionalFields");

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        if (additional != null) data.putAll(additional);

        return RestClient.builder()
                .url(BASE + "/tasks/" + taskId + "/subtasks")
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("data", data))
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:subtask:getAll")
    public Object getAllSubtasks(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(BASE + "/tasks/" + taskId + "/subtasks")
                .header("Authorization", auth(context))
                .get()
                .execute();
    }
}
