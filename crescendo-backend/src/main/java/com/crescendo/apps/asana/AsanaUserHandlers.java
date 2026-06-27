package com.crescendo.apps.asana;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

/**
 * Asana User handlers.
 */
@Component
public class AsanaUserHandlers {

    private static final String BASE = "https://app.asana.com/api/1.0";

    private String auth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:user:get")
    public Object getUser(ActionContext context) throws Exception {
        String userId = context.getString("userId");
        return RestClient.builder()
                .url(BASE + "/users/" + userId)
                .header("Authorization", auth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:user:getAll")
    public Object getAllUsers(ActionContext context) throws Exception {
        String workspaceId = context.getString("workspaceId");
        int limit = context.getInt("limit", 100);

        StringBuilder url = new StringBuilder(BASE + "/users?limit=" + limit);
        if (workspaceId != null && !workspaceId.isBlank()) url.append("&workspace=").append(workspaceId);

        return RestClient.builder()
                .url(url.toString())
                .header("Authorization", auth(context))
                .get()
                .execute();
    }
}
