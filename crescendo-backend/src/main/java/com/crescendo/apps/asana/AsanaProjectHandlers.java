package com.crescendo.apps.asana;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Asana Project handlers.
 * Operations (from n8n Asana.node.ts, resource='project'):
 *   - create : POST /1.0/projects
 *   - getAll : GET  /1.0/projects
 */
@Component
public class AsanaProjectHandlers {

    private static final String BASE = "https://app.asana.com/api/1.0";

    private String auth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:project:create")
    public Object createProject(ActionContext context) throws Exception {
        String name = context.getString("name");
        String workspaceId = context.getString("workspaceId");
        String teamId = context.getString("teamId");
        Map<String, Object> additional = context.getMap("additionalFields");

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        if (workspaceId != null) data.put("workspace", workspaceId);
        if (teamId != null) data.put("team", teamId);
        if (additional != null) data.putAll(additional);

        return RestClient.builder()
                .url(BASE + "/projects")
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("data", data))
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:project:getAll")
    public Object getAllProjects(ActionContext context) throws Exception {
        String workspaceId = context.getString("workspaceId");
        int limit = context.getInt("limit", 100);

        StringBuilder url = new StringBuilder(BASE + "/projects?limit=" + limit);
        if (workspaceId != null && !workspaceId.isBlank()) url.append("&workspace=").append(workspaceId);

        return RestClient.builder()
                .url(url.toString())
                .header("Authorization", auth(context))
                .get()
                .execute();
    }
}
