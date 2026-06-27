package com.crescendo.apps.gitlab;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * GitLab Issue handlers.
 */
@Component
public class GitLabIssueHandlers {

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:issue:create")
    public Object createIssue(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String title = context.getString("title");
        String description = context.getString("description");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        if (description != null) body.put("description", description);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/issues")
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:issue:update")
    public Object updateIssue(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String issueIid = context.getString("issueIid");
        Map<String, Object> fields = context.getMap("fields");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/issues/" + issueIid)
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .header("Content-Type", "application/json")
                .put(fields != null ? fields : Map.of())
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:issue:get")
    public Object getIssue(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String issueIid = context.getString("issueIid");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/issues/" + issueIid)
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:issue:getAll")
    public Object getAllIssues(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/issues")
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .get()
                .execute();
    }
}
