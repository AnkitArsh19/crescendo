package com.crescendo.apps.clickup;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ClickUp Time Entry handlers.
 */
@Component
public class ClickUpTimeEntryHandlers {

    private String getBaseUrl() {
        return "https://api.clickup.com/api/v2";
    }

    private String getAuth(ActionContext context) {
        return context.getCredential("apiToken");
    }

    // ─── TIME ENTRY ───

    @ActionMapping(appKey = "clickup", actionKey = "clickup:timeEntry:create")
    public Object createTimeEntry(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        String taskId = context.getString("taskId");

        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/time_entries")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("tid", taskId, "start", System.currentTimeMillis()))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:timeEntry:delete")
    public Object deleteTimeEntry(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        String timeEntryId = context.getString("timeEntryId");
        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/time_entries/" + timeEntryId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:timeEntry:get")
    public Object getTimeEntry(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        String timeEntryId = context.getString("timeEntryId");
        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/time_entries/" + timeEntryId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:timeEntry:getAll")
    public Object getAllTimeEntries(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/time_entries")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:timeEntry:update")
    public Object updateTimeEntry(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        String timeEntryId = context.getString("timeEntryId");

        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/time_entries/" + timeEntryId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(Map.of())
                .execute();
    }

    // ─── TIME ENTRY TAG ───

    @ActionMapping(appKey = "clickup", actionKey = "clickup:timeEntryTag:getAll")
    public Object getAllTimeEntryTags(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/time_entries/tags")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:timeEntryTag:remove")
    public Object removeTimeEntryTag(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        String timeEntryId = context.getString("timeEntryId");
        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/time_entries/" + timeEntryId + "/tags")
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
