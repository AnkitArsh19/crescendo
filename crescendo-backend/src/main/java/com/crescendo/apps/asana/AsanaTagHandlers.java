package com.crescendo.apps.asana;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Asana Tag handlers.
 */
@Component
public class AsanaTagHandlers {

    private static final String BASE = "https://app.asana.com/api/1.0";

    private String auth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    // ────────────────── TAG RESOURCE ──────────────────

    @ActionMapping(appKey = "asana", actionKey = "asana:tag:create")
    public Object createTag(ActionContext context) throws Exception {
        String workspaceId = context.getString("workspaceId");
        String name = context.getString("name");
        String color = context.getString("color");
        String notes = context.getString("notes");

        Map<String, Object> data = new HashMap<>();
        data.put("workspace", workspaceId);
        data.put("name", name);
        if (color != null) data.put("color", color);
        if (notes != null) data.put("notes", notes);

        return RestClient.builder()
                .url(BASE + "/tags")
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("data", data))
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:tag:get")
    public Object getTag(ActionContext context) throws Exception {
        String tagId = context.getString("tagId");
        return RestClient.builder()
                .url(BASE + "/tags/" + tagId)
                .header("Authorization", auth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:tag:getAll")
    public Object getAllTags(ActionContext context) throws Exception {
        String workspaceId = context.getString("workspaceId");
        int limit = context.getInt("limit", 100);

        return RestClient.builder()
                .url(BASE + "/tags?workspace=" + workspaceId + "&limit=" + limit)
                .header("Authorization", auth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:tag:update")
    public Object updateTag(ActionContext context) throws Exception {
        String tagId = context.getString("tagId");
        String name = context.getString("name");
        String color = context.getString("color");
        String notes = context.getString("notes");

        Map<String, Object> data = new HashMap<>();
        if (name != null) data.put("name", name);
        if (color != null) data.put("color", color);
        if (notes != null) data.put("notes", notes);

        return RestClient.builder()
                .url(BASE + "/tags/" + tagId)
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .put(Map.of("data", data))
                .execute();
    }

    // ────────────────── TASK TAG ──────────────────

    @ActionMapping(appKey = "asana", actionKey = "asana:taskTag:add")
    public Object addTagToTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String tagId = context.getString("tagId");

        return RestClient.builder()
                .url(BASE + "/tasks/" + taskId + "/addTag")
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("data", Map.of("tag", tagId)))
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:taskTag:remove")
    public Object removeTagFromTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String tagId = context.getString("tagId");

        return RestClient.builder()
                .url(BASE + "/tasks/" + taskId + "/removeTag")
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("data", Map.of("tag", tagId)))
                .execute();
    }
}
