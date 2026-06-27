package com.crescendo.apps.asana;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

/**
 * Asana Workspace and Team handlers.
 */
@Component
public class AsanaWorkspaceHandlers {

    private static final String BASE = "https://app.asana.com/api/1.0";

    private String auth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:workspace:getAll")
    public Object getAllWorkspaces(ActionContext context) throws Exception {
        int limit = context.getInt("limit", 100);

        return RestClient.builder()
                .url(BASE + "/workspaces?limit=" + limit)
                .header("Authorization", auth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:team:getAll")
    public Object getAllTeams(ActionContext context) throws Exception {
        String workspaceId = context.getString("workspaceId");
        int limit = context.getInt("limit", 100);

        StringBuilder url = new StringBuilder(BASE + "/workspaces/" + workspaceId + "/teams?limit=" + limit);

        return RestClient.builder()
                .url(url.toString())
                .header("Authorization", auth(context))
                .get()
                .execute();
    }
}
