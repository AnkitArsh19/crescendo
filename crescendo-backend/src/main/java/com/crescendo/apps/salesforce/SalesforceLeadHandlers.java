package com.crescendo.apps.salesforce;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SalesforceLeadHandlers {

    private String getBaseUrl(ActionContext context) {
        String instanceUrl = context.getCredential("instanceUrl");
        String apiVersion = context.getCredential("apiVersion");
        if (apiVersion == null || apiVersion.isBlank()) apiVersion = "v60.0";
        return instanceUrl + "/services/data/" + apiVersion;
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:lead:create")
    public Object createLead(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Lead")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:lead:upsert")
    public Object upsertLead(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String externalIdField = context.getString("externalId");
        String externalIdValue = context.getString("externalIdValue");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Lead/" + externalIdField + "/" + externalIdValue)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:lead:update")
    public Object updateLead(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String leadId = context.getString("leadId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Lead/" + leadId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:lead:get")
    public Object getLead(ActionContext context) throws Exception {
        String leadId = context.getString("leadId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Lead/" + leadId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:lead:getAll")
    public Object getAllLeads(ActionContext context) throws Exception {
        String query = "SELECT Id, Name, Company FROM Lead";
        if (context.getString("fields") != null) query = "SELECT " + context.getString("fields") + " FROM Lead";
        if (context.getInt("limit") != null) query += " LIMIT " + context.getInt("limit");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/query?q=" + java.net.URLEncoder.encode(query, "UTF-8"))
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:lead:delete")
    public Object deleteLead(ActionContext context) throws Exception {
        String leadId = context.getString("leadId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Lead/" + leadId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:lead:addNote")
    public Object addNoteToLead(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Note")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:lead:getSummary")
    public Object getLeadSummary(ActionContext context) throws Exception {
        String leadId = context.getString("leadId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Lead/" + leadId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:lead:addToCampaign")
    public Object addToCampaign(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/CampaignMember")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }
}
