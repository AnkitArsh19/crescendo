package com.crescendo.apps.clickup;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ClickUp Checklist and Checklist Item handlers.
 */
@Component
public class ClickUpChecklistHandlers {

    private String getBaseUrl() {
        return "https://api.clickup.com/api/v2";
    }

    private String getAuth(ActionContext context) {
        return context.getCredential("apiToken");
    }

    // ─── CHECKLIST ───
    
    @ActionMapping(appKey = "clickup", actionKey = "clickup:checklist:create")
    public Object createChecklist(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(getBaseUrl() + "/task/" + taskId + "/checklist")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:checklist:delete")
    public Object deleteChecklist(ActionContext context) throws Exception {
        String checklistId = context.getString("checklistId");
        return RestClient.builder()
                .url(getBaseUrl() + "/checklist/" + checklistId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:checklist:update")
    public Object updateChecklist(ActionContext context) throws Exception {
        String checklistId = context.getString("checklistId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(getBaseUrl() + "/checklist/" + checklistId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(Map.of("name", name))
                .execute();
    }

    // ─── CHECKLIST ITEM ───
    
    @ActionMapping(appKey = "clickup", actionKey = "clickup:checklistItem:create")
    public Object createChecklistItem(ActionContext context) throws Exception {
        String checklistId = context.getString("checklistId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(getBaseUrl() + "/checklist/" + checklistId + "/checklist_item")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:checklistItem:delete")
    public Object deleteChecklistItem(ActionContext context) throws Exception {
        String checklistId = context.getString("checklistId");
        String checklistItemId = context.getString("checklistItemId");
        return RestClient.builder()
                .url(getBaseUrl() + "/checklist/" + checklistId + "/checklist_item/" + checklistItemId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:checklistItem:update")
    public Object updateChecklistItem(ActionContext context) throws Exception {
        String checklistId = context.getString("checklistId");
        String checklistItemId = context.getString("checklistItemId");
        String name = context.getString("name");

        Map<String, Object> body = name != null ? Map.of("name", name) : Map.of();

        return RestClient.builder()
                .url(getBaseUrl() + "/checklist/" + checklistId + "/checklist_item/" + checklistItemId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }
}
