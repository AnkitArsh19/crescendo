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
 * GitLab Webhook handlers.
 */
@Component
public class GitLabWebhookHandlers {

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:webhook:create")
    public Object createWebhook(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String url = context.getString("webhookUrl");
        String secretToken = context.getString("secretToken");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        Map<String, Object> body = new HashMap<>();
        body.put("url", url);
        if (secretToken != null) body.put("token", secretToken);
        
        // Example defaults based on n8n
        body.put("push_events", true);
        body.put("issues_events", true);
        body.put("merge_requests_events", true);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/hooks")
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "gitlab", actionKey = "gitlab:webhook:delete")
    public Object deleteWebhook(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String hookId = context.getString("hookId");
        String encodedId = URLEncoder.encode(projectId, StandardCharsets.UTF_8);

        return RestClient.builder()
                .url(GitLabSupport.getBaseUrl(context) + "/projects/" + encodedId + "/hooks/" + hookId)
                .header("Authorization", GitLabSupport.getAuthHeader(context))
                .delete()
                .execute();
    }
}
