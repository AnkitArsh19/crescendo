package com.crescendo.apps.jira;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Jira Attachment handlers.
 */
@Component
public class JiraAttachmentHandlers {

    private String getBaseUrl(ActionContext context) {
        String domain = context.getCredential("domain");
        if (domain != null && domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain + "/rest/api/3";
    }

    private String getAuth(ActionContext context) {
        String email = context.getCredential("email");
        String token = context.getCredential("apiToken");
        return "Basic " + Base64.getEncoder().encodeToString((email + ":" + token).getBytes());
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueAttachment:add")
    public Object addAttachment(ActionContext context) throws Exception {
// String issueKey = context.getString("issueKey");
        throw new UnsupportedOperationException("Multipart form-data uploads require extended RestClient support.");
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueAttachment:get")
    public Object getAttachment(ActionContext context) throws Exception {
        String attachmentId = context.getString("attachmentId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/attachment/" + attachmentId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueAttachment:getAll")
    public Object getAllAttachments(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey + "?fields=attachment")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueAttachment:remove")
    public Object removeAttachment(ActionContext context) throws Exception {
        String attachmentId = context.getString("attachmentId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/attachment/" + attachmentId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
