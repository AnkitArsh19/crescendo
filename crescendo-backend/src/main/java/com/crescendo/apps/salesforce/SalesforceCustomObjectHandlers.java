package com.crescendo.apps.salesforce;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SalesforceCustomObjectHandlers {

    private String getBaseUrl(ActionContext context) {
        String instanceUrl = context.getCredential("instanceUrl");
        String apiVersion = context.getCredential("apiVersion");
        if (apiVersion == null || apiVersion.isBlank()) apiVersion = "v60.0";
        return instanceUrl + "/services/data/" + apiVersion;
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:customObject:create")
    public Object createCustomObject(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String objectName = context.getString("customObject");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/" + objectName)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:customObject:update")
    public Object updateCustomObject(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String objectName = context.getString("customObject");
        String recordId = context.getString("recordId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/" + objectName + "/" + recordId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:customObject:upsert")
    public Object upsertCustomObject(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String objectName = context.getString("customObject");
        String externalIdField = context.getString("externalId");
        String externalIdValue = context.getString("externalIdValue");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/" + objectName + "/" + externalIdField + "/" + externalIdValue)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:customObject:get")
    public Object getCustomObject(ActionContext context) throws Exception {
        String objectName = context.getString("customObject");
        String recordId = context.getString("recordId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/" + objectName + "/" + recordId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:customObject:getAll")
    public Object getAllCustomObjects(ActionContext context) throws Exception {
        String objectName = context.getString("customObject");
        String query = "SELECT Id FROM " + objectName;
        if (context.getString("fields") != null) query = "SELECT " + context.getString("fields") + " FROM " + objectName;
        if (context.getInt("limit") != null) query += " LIMIT " + context.getInt("limit");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/query?q=" + java.net.URLEncoder.encode(query, "UTF-8"))
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:customObject:delete")
    public Object deleteCustomObject(ActionContext context) throws Exception {
        String objectName = context.getString("customObject");
        String recordId = context.getString("recordId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/" + objectName + "/" + recordId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .delete()
                .execute();
    }
}
