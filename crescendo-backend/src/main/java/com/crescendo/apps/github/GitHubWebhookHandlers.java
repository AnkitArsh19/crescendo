package com.crescendo.apps.github;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub Webhook handlers.
 */
@Component
public class GitHubWebhookHandlers {

    @ActionMapping(appKey = "github", actionKey = "github:webhook:create")
    public Object createWebhook(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String webhookUrl = context.getString("webhookUrl");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) context.configuration().get("events");
        String secret = context.getString("secret");

        Map<String, Object> config = new HashMap<>();
        config.put("url", webhookUrl);
        config.put("content_type", "json");
        if (secret != null) config.put("secret", secret);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "web");
        body.put("active", true);
        if (events != null) body.put("events", events);
        body.put("config", config);

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/hooks")
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "github", actionKey = "github:webhook:delete")
    public Object deleteWebhook(ActionContext context) throws Exception {
        String owner = context.getString("owner");
        String repo = context.getString("repo");
        String hookId = context.getString("hookId");

        return RestClient.builder()
                .url(GitHubSupport.getBaseUrl() + "/repos/" + owner + "/" + repo + "/hooks/" + hookId)
                .header("Authorization", GitHubSupport.getAuthHeader(context))
                .header("Accept", "application/vnd.github.v3+json")
                .delete()
                .execute();
    }
}
