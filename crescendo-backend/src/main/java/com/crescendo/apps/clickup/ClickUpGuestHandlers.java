package com.crescendo.apps.clickup;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ClickUp Guest handlers.
 */
@Component
public class ClickUpGuestHandlers {

    private String getBaseUrl() {
        return "https://api.clickup.com/api/v2";
    }

    private String getAuth(ActionContext context) {
        return context.getCredential("apiToken");
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:guest:create")
    public Object createGuest(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        String email = context.getString("email");

        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/guest")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("email", email))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:guest:delete")
    public Object deleteGuest(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        String guestId = context.getString("guestId");
        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/guest/" + guestId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:guest:get")
    public Object getGuest(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        String guestId = context.getString("guestId");
        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/guest/" + guestId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:guest:update")
    public Object updateGuest(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        String guestId = context.getString("guestId");

        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/guest/" + guestId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(Map.of())
                .execute();
    }
}
