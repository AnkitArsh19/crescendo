package com.crescendo.apps.salesforce;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SalesforceCaseHandlers {

    private String getBaseUrl(ActionContext context) {
        String instanceUrl = context.getCredential("instanceUrl");
        String apiVersion = context.getCredential("apiVersion");
        if (apiVersion == null || apiVersion.isBlank()) apiVersion = "v60.0";
        return instanceUrl + "/services/data/" + apiVersion;
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:case:create")
    public Object createCase(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Case")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:case:update")
    public Object updateCase(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String caseId = context.getString("caseId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Case/" + caseId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:case:get")
    public Object getCase(ActionContext context) throws Exception {
        String caseId = context.getString("caseId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Case/" + caseId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:case:getAll")
    public Object getAllCases(ActionContext context) throws Exception {
        String query = "SELECT Id, CaseNumber, Subject, Status FROM Case";
        if (context.getString("fields") != null) query = "SELECT " + context.getString("fields") + " FROM Case";
        if (context.getInt("limit") != null) query += " LIMIT " + context.getInt("limit");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/query?q=" + java.net.URLEncoder.encode(query, "UTF-8"))
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:case:delete")
    public Object deleteCase(ActionContext context) throws Exception {
        String caseId = context.getString("caseId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Case/" + caseId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:case:addComment")
    public Object addCommentToCase(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        // Typically a CaseComment in Salesforce
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/CaseComment")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:case:getSummary")
    public Object getCaseSummary(ActionContext context) throws Exception {
        String caseId = context.getString("caseId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Case/" + caseId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }
}
