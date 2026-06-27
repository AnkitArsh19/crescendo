package com.crescendo.apps.gitlab;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * GitLab Repository (Project) handlers.
 */
@Component
public class GitLabRepoHandlers {

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:repo:get")
    public Object getRepo(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId)
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:repo:getAll")
    public Object getAllRepos(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects?membership=true")
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .get()
                .execute();
    }
}
