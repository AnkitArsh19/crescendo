package com.crescendo.apps.salesforce;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SalesforceOpportunityHandlers {

    private String getBaseUrl(ActionContext context) {
        String instanceUrl = context.getCredential("instanceUrl");
        String apiVersion = context.getCredential("apiVersion");
        if (apiVersion == null || apiVersion.isBlank()) apiVersion = "v60.0";
        return instanceUrl + "/services/data/" + apiVersion;
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:opportunity:create")
    public Object createOpportunity(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Opportunity")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:opportunity:upsert")
    public Object upsertOpportunity(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String externalIdField = context.getString("externalId");
        String externalIdValue = context.getString("externalIdValue");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Opportunity/" + externalIdField + "/" + externalIdValue)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:opportunity:update")
    public Object updateOpportunity(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String opportunityId = context.getString("opportunityId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Opportunity/" + opportunityId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:opportunity:get")
    public Object getOpportunity(ActionContext context) throws Exception {
        String opportunityId = context.getString("opportunityId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Opportunity/" + opportunityId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:opportunity:getAll")
    public Object getAllOpportunities(ActionContext context) throws Exception {
        String query = "SELECT Id, Name, StageName, Amount FROM Opportunity";
        if (context.getString("fields") != null) query = "SELECT " + context.getString("fields") + " FROM Opportunity";
        if (context.getInt("limit") != null) query += " LIMIT " + context.getInt("limit");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/query?q=" + java.net.URLEncoder.encode(query, "UTF-8"))
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:opportunity:delete")
    public Object deleteOpportunity(ActionContext context) throws Exception {
        String opportunityId = context.getString("opportunityId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Opportunity/" + opportunityId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:opportunity:addNote")
    public Object addNoteToOpportunity(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Note")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:opportunity:getSummary")
    public Object getOpportunitySummary(ActionContext context) throws Exception {
        String opportunityId = context.getString("opportunityId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Opportunity/" + opportunityId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }
}
