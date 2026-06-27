package com.crescendo.apps.clickup;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * ClickUp List handlers.
 */
@Component
public class ClickUpListHandlers {

    private String getBaseUrl() {
        return "https://api.clickup.com/api/v2";
    }

    private String getAuth(ActionContext context) {
        return context.getCredential("apiToken");
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:list:create")
    public Object createList(ActionContext context) throws Exception {
        String folderId = context.getString("folderId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(getBaseUrl() + "/folder/" + folderId + "/list")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:list:delete")
    public Object deleteList(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        return RestClient.builder()
                .url(getBaseUrl() + "/list/" + listId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:list:get")
    public Object getList(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        return RestClient.builder()
                .url(getBaseUrl() + "/list/" + listId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:list:getAll")
    public Object getAllLists(ActionContext context) throws Exception {
        String folderId = context.getString("folderId");
        return RestClient.builder()
                .url(getBaseUrl() + "/folder/" + folderId + "/list")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:list:update")
    public Object updateList(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String name = context.getString("name");

        Map<String, Object> body = new HashMap<>();
        if (name != null) body.put("name", name);

        return RestClient.builder()
                .url(getBaseUrl() + "/list/" + listId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }
}
