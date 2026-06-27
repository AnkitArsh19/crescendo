package com.crescendo.apps.salesforce;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SalesforceTaskHandlers {

    private String getBaseUrl(ActionContext context) {
        String instanceUrl = context.getCredential("instanceUrl");
        String apiVersion = context.getCredential("apiVersion");
        if (apiVersion == null || apiVersion.isBlank()) apiVersion = "v60.0";
        return instanceUrl + "/services/data/" + apiVersion;
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:task:create")
    public Object createTask(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Task")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:task:update")
    public Object updateTask(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Task/" + taskId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:task:get")
    public Object getTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Task/" + taskId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:task:getAll")
    public Object getAllTasks(ActionContext context) throws Exception {
        String query = "SELECT Id, Subject, Status, Priority FROM Task";
        if (context.getString("fields") != null) query = "SELECT " + context.getString("fields") + " FROM Task";
        if (context.getInt("limit") != null) query += " LIMIT " + context.getInt("limit");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/query?q=" + java.net.URLEncoder.encode(query, "UTF-8"))
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:task:delete")
    public Object deleteTask(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Task/" + taskId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:task:getSummary")
    public Object getTaskSummary(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Task/" + taskId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }
}
