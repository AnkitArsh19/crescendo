package com.crescendo.apps.salesforce;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SalesforceAccountHandlers {

    private String getBaseUrl(ActionContext context) {
        String instanceUrl = context.getCredential("instanceUrl");
        String apiVersion = context.getCredential("apiVersion");
        if (apiVersion == null || apiVersion.isBlank()) apiVersion = "v60.0";
        return instanceUrl + "/services/data/" + apiVersion;
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:account:create")
    public Object createAccount(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Account")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:account:upsert")
    public Object upsertAccount(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String externalIdField = context.getString("externalId");
        String externalIdValue = context.getString("externalIdValue");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Account/" + externalIdField + "/" + externalIdValue)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:account:update")
    public Object updateAccount(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String accountId = context.getString("accountId");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Account/" + accountId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:account:get")
    public Object getAccount(ActionContext context) throws Exception {
        String accountId = context.getString("accountId");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Account/" + accountId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:account:getAll")
    public Object getAllAccounts(ActionContext context) throws Exception {
        String query = "SELECT Id, Name FROM Account"; // Default simple query
        if (context.getString("fields") != null) {
            query = "SELECT " + context.getString("fields") + " FROM Account";
        }
        if (context.getInt("limit") != null) {
            query += " LIMIT " + context.getInt("limit");
        }

        return RestClient.builder()
                .url(getBaseUrl(context) + "/query?q=" + java.net.URLEncoder.encode(query, "UTF-8"))
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:account:delete")
    public Object deleteAccount(ActionContext context) throws Exception {
        String accountId = context.getString("accountId");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Account/" + accountId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:account:addNote")
    public Object addNoteToAccount(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        // Creates a Note attached to the account
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Note")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:account:getSummary")
    public Object getAccountSummary(ActionContext context) throws Exception {
        String accountId = context.getString("accountId");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Account/" + accountId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }
}
