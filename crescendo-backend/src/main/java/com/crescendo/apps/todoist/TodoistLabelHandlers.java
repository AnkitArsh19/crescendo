package com.crescendo.apps.todoist;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Todoist Label handlers.
 */
@Component
public class TodoistLabelHandlers {

    private String getBaseUrl() {
        return "https://api.todoist.com/rest/v2";
    }

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("apiKey");
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:label:create")
    public Object createLabel(ActionContext context) throws Exception {
        String name = context.getString("name");
        return RestClient.builder()
                .url(getBaseUrl() + "/labels")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:label:get")
    public Object getLabel(ActionContext context) throws Exception {
        String labelId = context.getString("labelId");
        return RestClient.builder()
                .url(getBaseUrl() + "/labels/" + labelId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:label:getAll")
    public Object getAllLabels(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(getBaseUrl() + "/labels")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:label:update")
    public Object updateLabel(ActionContext context) throws Exception {
        String labelId = context.getString("labelId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(getBaseUrl() + "/labels/" + labelId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:label:delete")
    public Object deleteLabel(ActionContext context) throws Exception {
        String labelId = context.getString("labelId");
        return RestClient.builder()
                .url(getBaseUrl() + "/labels/" + labelId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
