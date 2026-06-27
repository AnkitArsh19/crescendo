package com.crescendo.apps.github;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * GitHub Release handlers.
 */
@Component
public class GitHubReleaseHandlers {

    @ActionMapping(appKey = "github", actionKey = "github:release:create")
    public Object createRelease(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String tagName = context.getString("tagName");
        String name = context.getString("name");
        String bodyText = context.getString("body");

        Map<String, Object> body = new HashMap<>();
        body.put("tag_name", tagName);
        if (name != null) body.put("name", name);
        if (bodyText != null) body.put("body", bodyText);

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/releases")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:release:get")
    public Object getRelease(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String releaseId = context.getString("releaseId");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/releases/" + releaseId)
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:release:getAll")
    public Object getAllReleases(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/releases")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:release:delete")
    public Object deleteRelease(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String releaseId = context.getString("releaseId");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/releases/" + releaseId)
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .delete()
                .execute();
    }
}
