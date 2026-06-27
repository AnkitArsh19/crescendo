package com.crescendo.apps.github;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * GitHub File handlers.
 */
@Component
public class GitHubFileHandlers {

    @ActionMapping(appKey = "github", actionKey = "github:file:create")
    public Object createFile(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String path = context.getString("path");
        String message = context.getString("message");
        String content = context.getString("content");

        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("content", content); // requires Base64 encoding usually, assuming content is base64
        
        String branch = context.getString("branch");
        if (branch != null) body.put("branch", branch);

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/contents/" + path)
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:file:update")
    public Object updateFile(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String path = context.getString("path");
        String message = context.getString("message");
        String content = context.getString("content");
        String sha = context.getString("sha");

        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("content", content);
        body.put("sha", sha);
        
        String branch = context.getString("branch");
        if (branch != null) body.put("branch", branch);

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/contents/" + path)
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:file:get")
    public Object getFile(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String path = context.getString("path");
        String ref = context.getString("ref");

        String url = GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/contents/" + path;
        if (ref != null) url += "?ref=" + ref;

        return RestClient.builder()
                .url(url)
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:file:delete")
    public Object deleteFile(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String path = context.getString("path");
        String message = context.getString("message");
        String sha = context.getString("sha");

        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("sha", sha);
        
        String branch = context.getString("branch");
        if (branch != null) body.put("branch", branch);

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/contents/" + path)
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .delete().body(body) // using body for DELETE since github needs message and sha
                .execute();
    }
}
