package com.crescendo.apps.salesforce;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SalesforceAttachmentHandlers {

    private String getBaseUrl(ActionContext context) {
        String instanceUrl = context.getCredential("instanceUrl");
        String apiVersion = context.getCredential("apiVersion");
        if (apiVersion == null || apiVersion.isBlank()) apiVersion = "v60.0";
        return instanceUrl + "/services/data/" + apiVersion;
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:attachment:create")
    public Object createAttachment(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Attachment")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:attachment:update")
    public Object updateAttachment(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String attachmentId = context.getString("attachmentId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Attachment/" + attachmentId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .patch(payload)
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:attachment:get")
    public Object getAttachment(ActionContext context) throws Exception {
        String attachmentId = context.getString("attachmentId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Attachment/" + attachmentId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:attachment:getAll")
    public Object getAllAttachments(ActionContext context) throws Exception {
        String query = "SELECT Id, Name, ContentType FROM Attachment";
        if (context.getString("fields") != null) query = "SELECT " + context.getString("fields") + " FROM Attachment";
        if (context.getInt("limit") != null) query += " LIMIT " + context.getInt("limit");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/query?q=" + java.net.URLEncoder.encode(query, "UTF-8"))
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:attachment:delete")
    public Object deleteAttachment(ActionContext context) throws Exception {
        String attachmentId = context.getString("attachmentId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Attachment/" + attachmentId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:attachment:getSummary")
    public Object getAttachmentSummary(ActionContext context) throws Exception {
        String attachmentId = context.getString("attachmentId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/sobjects/Attachment/" + attachmentId)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }
}
