package com.crescendo.apps.github;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * GitHub Repository handlers.
 */
@Component
public class GitHubRepoHandlers {

    @ActionMapping(appKey = "github", actionKey = "github:repo:get")
    public Object getRepo(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo)
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:repo:getAll")
    public Object getAllRepos(ActionContext context) throws Exception {
        // Fetches repos for the authenticated user
        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/user/repos")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:repo:listForks")
    public Object listForks(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/forks")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:repo:createFork")
    public Object createFork(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/forks")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .post(Map.of()) // empty body
                .execute();
    }
}
