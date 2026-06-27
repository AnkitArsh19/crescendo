package com.crescendo.apps.salesforce;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SalesforceContactHandlers {

    private String getBaseUrl(ActionContext context) {
        String instanceUrl = context.getCredential("instanceUrl");
        String apiVersion = context.getCredential("apiVersion");
        if (apiVersion == null || apiVersion.isBlank()) apiVersion = "v60.0";
        return instanceUrl + "/services/data/" + apiVersion;
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:contact:create")
    public Object createContact(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Contact")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:contact:upsert")
    public Object upsertContact(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String externalIdField = context.getString("externalId");
        String externalIdValue = context.getString("externalIdValue");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Contact/" + externalIdField + "/" + externalIdValue)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:contact:update")
    public Object updateContact(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String contactId = context.getString("contactId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Contact/" + contactId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:contact:get")
    public Object getContact(ActionContext context) throws Exception {
        String contactId = context.getString("contactId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Contact/" + contactId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:contact:getAll")
    public Object getAllContacts(ActionContext context) throws Exception {
        String query = "SELECT Id, Name, Email FROM Contact";
        if (context.getString("fields") != null) query = "SELECT " + context.getString("fields") + " FROM Contact";
        if (context.getInt("limit") != null) query += " LIMIT " + context.getInt("limit");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/query?q=" + java.net.URLEncoder.encode(query, "UTF-8"))
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:contact:delete")
    public Object deleteContact(ActionContext context) throws Exception {
        String contactId = context.getString("contactId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Contact/" + contactId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:contact:addNote")
    public Object addNoteToContact(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Note")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:contact:getSummary")
    public Object getContactSummary(ActionContext context) throws Exception {
        String contactId = context.getString("contactId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Contact/" + contactId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:contact:addToCampaign")
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
