package com.crescendo.apps.github;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub Pull Request handlers.
 */
@Component
public class GitHubPRHandlers {

    @ActionMapping(appKey = "github", actionKey = "github:pullRequest:create")
    public Object createPullRequest(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String title = context.getString("title");
        String head = context.getString("head");
        String base = context.getString("base");
        String bodyText = context.getString("body");

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("head", head);
        body.put("base", base);
        if (bodyText != null) body.put("body", bodyText);

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/pulls")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:pullRequest:get")
    public Object getPullRequest(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String prNumber = context.getString("pullNumber");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/pulls/" + prNumber)
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:pullRequest:getAll")
    public Object getAllPullRequests(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/pulls")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:pullRequest:merge")
    public Object mergePullRequest(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String prNumber = context.getString("pullNumber");
        String commitTitle = context.getString("commitTitle");
        String mergeMethod = context.getString("mergeMethod");

        Map<String, Object> body = new HashMap<>();
        if (commitTitle != null) body.put("commit_title", commitTitle);
        if (mergeMethod != null) body.put("merge_method", mergeMethod);

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/pulls/" + prNumber + "/merge")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:pullRequest:addReviewer")
    public Object addReviewer(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String prNumber = context.getString("pullNumber");
        @SuppressWarnings("unchecked")
        List<String> reviewers = (List<String>) context.configuration().get("reviewers");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/pulls/" + prNumber + "/requested_reviewers")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .post(Map.of("reviewers", reviewers))
                .execute();
    }
}
