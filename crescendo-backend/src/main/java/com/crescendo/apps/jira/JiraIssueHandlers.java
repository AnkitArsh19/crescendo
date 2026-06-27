package com.crescendo.apps.jira;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Jira Issue handlers.
 * Operations: changelog, create, delete, get, getAll, notify, update
 */
@Component
public class JiraIssueHandlers {

    private String getBaseUrl(ActionContext context) {
        String domain = context.getCredential("domain");
        if (domain != null && domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain + "/rest/api/3"; // using v3 for Jira Cloud
    }

    private String getAuth(ActionContext context) {
        String email = context.getCredential("email");
        String token = context.getCredential("apiToken");
        String authStr = email + ":" + token;
        return "Basic " + Base64.getEncoder().encodeToString(authStr.getBytes());
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issue:create")
    public Object createIssue(ActionContext context) throws Exception {
        String projectKey = context.getString("projectKey");
        String issueType = context.getString("issueType");
        String summary = context.getString("summary");
        Map<String, Object> additionalFields = context.getMap("additionalFields");

        Map<String, Object> fields = new HashMap<>();
        fields.put("project", Map.of("key", projectKey));
        fields.put("issuetype", Map.of("id", issueType));
        fields.put("summary", summary);
        if (additionalFields != null) {
            fields.putAll(additionalFields);
        }

        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("fields", fields))
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issue:update")
    public Object updateIssue(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        Map<String, Object> updateFields = context.getMap("updateFields");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(Map.of("fields", updateFields != null ? updateFields : Map.of()))
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issue:delete")
    public Object deleteIssue(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issue:get")
    public Object getIssue(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issue:getAll")
    public Object getAllIssues(ActionContext context) throws Exception {
        String jql = context.getString("jql");
        int limit = context.getInt("limit", 50);

        Map<String, Object> body = new HashMap<>();
        if (jql != null && !jql.isBlank()) body.put("jql", jql);
        body.put("maxResults", limit);

        return RestClient.builder()
                .url(getBaseUrl(context) + "/search")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issue:changelog")
    public Object getIssueChangelog(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey + "/changelog")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issue:notify")
    public Object notifyIssue(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        Map<String, Object> body = context.getMap("body");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey + "/notify")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }
}
