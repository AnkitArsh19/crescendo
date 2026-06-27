package com.crescendo.apps.clickup;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ClickUp Space Tag handlers.
 */
@Component
public class ClickUpSpaceTagHandlers {

    private String getBaseUrl() {
        return "https://api.clickup.com/api/v2";
    }

    private String getAuth(ActionContext context) {
        return context.getCredential("apiToken");
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:spaceTag:create")
    public Object createSpaceTag(ActionContext context) throws Exception {
        String spaceId = context.getString("spaceId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(getBaseUrl() + "/space/" + spaceId + "/tag")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("tag", Map.of("name", name)))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:spaceTag:delete")
    public Object deleteSpaceTag(ActionContext context) throws Exception {
        String spaceId = context.getString("spaceId");
        String name = context.getString("name");
        return RestClient.builder()
                .url(getBaseUrl() + "/space/" + spaceId + "/tag/" + name)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:spaceTag:getAll")
    public Object getAllSpaceTags(ActionContext context) throws Exception {
        String spaceId = context.getString("spaceId");
        return RestClient.builder()
                .url(getBaseUrl() + "/space/" + spaceId + "/tag")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:spaceTag:update")
    public Object updateSpaceTag(ActionContext context) throws Exception {
        String spaceId = context.getString("spaceId");
        String name = context.getString("name");
        String newName = context.getString("newName");

        return RestClient.builder()
                .url(getBaseUrl() + "/space/" + spaceId + "/tag/" + name)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(Map.of("tag", Map.of("name", newName != null ? newName : name)))
                .execute();
    }
}
