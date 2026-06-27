package com.crescendo.apps.clickup;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * ClickUp Folder handlers.
 */
@Component
public class ClickUpFolderHandlers {

    private String getBaseUrl() {
        return "https://api.clickup.com/api/v2";
    }

    private String getAuth(ActionContext context) {
        return context.getCredential("apiToken");
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:folder:create")
    public Object createFolder(ActionContext context) throws Exception {
        String spaceId = context.getString("spaceId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(getBaseUrl() + "/space/" + spaceId + "/folder")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:folder:delete")
    public Object deleteFolder(ActionContext context) throws Exception {
        String folderId = context.getString("folderId");
        return RestClient.builder()
                .url(getBaseUrl() + "/folder/" + folderId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:folder:get")
    public Object getFolder(ActionContext context) throws Exception {
        String folderId = context.getString("folderId");
        return RestClient.builder()
                .url(getBaseUrl() + "/folder/" + folderId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:folder:getAll")
    public Object getAllFolders(ActionContext context) throws Exception {
        String spaceId = context.getString("spaceId");
        return RestClient.builder()
                .url(getBaseUrl() + "/space/" + spaceId + "/folder")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:folder:update")
    public Object updateFolder(ActionContext context) throws Exception {
        String folderId = context.getString("folderId");
        String name = context.getString("name");

        Map<String, Object> body = new HashMap<>();
        if (name != null) body.put("name", name);

        return RestClient.builder()
                .url(getBaseUrl() + "/folder/" + folderId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }
}
