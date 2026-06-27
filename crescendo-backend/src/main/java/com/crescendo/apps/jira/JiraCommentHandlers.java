package com.crescendo.apps.jira;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

/**
 * Jira Comment handlers.
 */
@Component
public class JiraCommentHandlers {

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

    @ActionMapping(appKey = "jira", actionKey = "jira:issueComment:add")
    public Object addComment(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        String body = context.getString("body");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey + "/comment")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("body", body))
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueComment:get")
    public Object getComment(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        String commentId = context.getString("commentId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey + "/comment/" + commentId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueComment:getAll")
    public Object getAllComments(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey + "/comment")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueComment:update")
    public Object updateComment(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        String commentId = context.getString("commentId");
        String body = context.getString("body");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey + "/comment/" + commentId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(Map.of("body", body))
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueComment:remove")
    public Object removeComment(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        String commentId = context.getString("commentId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey + "/comment/" + commentId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
