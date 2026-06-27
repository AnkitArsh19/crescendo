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
 * GitLab Release handlers.
 */
@Component
public class GitLabReleaseHandlers {

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:release:create")
    public Object createRelease(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String tagName = context.getString("tagName");
        String name = context.getString("name");
        String description = context.getString("description");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("tag_name", tagName);
        if (description != null) body.put("description", description);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/releases")
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:release:get")
    public Object getRelease(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String tagName = context.getString("tagName");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);
        String encodedTag = URLEncoder.encode(tagName, StandardCharsets.UTF_8);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/releases/" + encodedTag)
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:release:getAll")
    public Object getAllReleases(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/releases")
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:release:delete")
    public Object deleteRelease(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String tagName = context.getString("tagName");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);
        String encodedTag = URLEncoder.encode(tagName, StandardCharsets.UTF_8);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/releases/" + encodedTag)
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .delete()
                .execute();
    }
}
