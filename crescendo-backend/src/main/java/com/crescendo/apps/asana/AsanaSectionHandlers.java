package com.crescendo.apps.asana;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Asana Section handlers.
 */
@Component
public class AsanaSectionHandlers {

    private static final String BASE = "https://app.asana.com/api/1.0";

    private String auth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:section:create")
    public Object createSection(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(BASE + "/projects/" + projectId + "/sections")
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("data", Map.of("name", name)))
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:section:delete")
    public Object deleteSection(ActionContext context) throws Exception {
        String sectionId = context.getString("sectionId");
        return RestClient.builder()
                .url(BASE + "/sections/" + sectionId)
                .header("Authorization", auth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:section:get")
    public Object getSection(ActionContext context) throws Exception {
        String sectionId = context.getString("sectionId");
        return RestClient.builder()
                .url(BASE + "/sections/" + sectionId)
                .header("Authorization", auth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:section:getAll")
    public Object getAllSections(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        int limit = context.getInt("limit", 100);

        return RestClient.builder()
                .url(BASE + "/projects/" + projectId + "/sections?limit=" + limit)
                .header("Authorization", auth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:section:update")
    public Object updateSection(ActionContext context) throws Exception {
        String sectionId = context.getString("sectionId");
        String name = context.getString("name");

        Map<String, Object> data = new HashMap<>();
        if (name != null) data.put("name", name);

        return RestClient.builder()
                .url(BASE + "/sections/" + sectionId)
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .put(Map.of("data", data))
                .execute();
    }
}
