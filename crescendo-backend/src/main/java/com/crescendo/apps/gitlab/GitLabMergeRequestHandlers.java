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
 * GitLab Merge Request handlers.
 */
@Component
public class GitLabMergeRequestHandlers {

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:mergeRequest:create")
    public Object createMergeRequest(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String sourceBranch = context.getString("sourceBranch");
        String targetBranch = context.getString("targetBranch");
        String title = context.getString("title");
        String description = context.getString("description");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        Map<String, Object> body = new HashMap<>();
        body.put("source_branch", sourceBranch);
        body.put("target_branch", targetBranch);
        body.put("title", title);
        if (description != null) body.put("description", description);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/merge_requests")
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:mergeRequest:get")
    public Object getMergeRequest(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String mergeRequestIid = context.getString("mergeRequestIid");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/merge_requests/" + mergeRequestIid)
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:mergeRequest:getAll")
    public Object getAllMergeRequests(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/merge_requests")
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:mergeRequest:update")
    public Object updateMergeRequest(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String mergeRequestIid = context.getString("mergeRequestIid");
        Map<String, Object> fields = context.getMap("fields");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/merge_requests/" + mergeRequestIid)
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .header("Content-Type", "application/json")
                .put(fields != null ? fields : Map.of())
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:mergeRequest:merge")
    public Object acceptMergeRequest(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String mergeRequestIid = context.getString("mergeRequestIid");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);
        Map<String, Object> body = context.getMap("body");

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/merge_requests/" + mergeRequestIid + "/merge")
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .header("Content-Type", "application/json")
                .put(body != null ? body : Map.of())
                .execute();
    }
}
