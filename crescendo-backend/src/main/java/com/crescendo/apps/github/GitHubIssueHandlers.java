package com.crescendo.apps.github;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub Issue handlers.
 */
@Component
public class GitHubIssueHandlers {

    @ActionMapping(appKey = "github", actionKey = "github:issue:create")
    public Object createIssue(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String title = context.getString("title");
        String bodyText = context.getString("body");

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        if (bodyText != null) body.put("body", bodyText);

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/issues")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:issue:update")
    public Object updateIssue(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String issueNumber = context.getString("issueNumber");
        Map<String, Object> fields = context.getMap("fields");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/issues/" + issueNumber)
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .post(fields != null ? fields : Map.of()) // github uses PATCH for issues but our rest client might use post/put
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:issue:get")
    public Object getIssue(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String issueNumber = context.getString("issueNumber");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/issues/" + issueNumber)
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:issue:getAll")
    public Object getAllIssues(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/issues")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:issue:addComment")
    public Object addComment(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String issueNumber = context.getString("issueNumber");
        String body = context.getString("body");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/issues/" + issueNumber + "/comments")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .post(Map.of("body", body))
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:issue:addLabel")
    public Object addLabel(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String issueNumber = context.getString("issueNumber");
        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) context.configuration().get("labels");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/issues/" + issueNumber + "/labels")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .post(Map.of("labels", labels))
                .execute();
    }
}
