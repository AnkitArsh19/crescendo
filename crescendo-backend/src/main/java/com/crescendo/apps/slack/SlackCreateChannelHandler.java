package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a new Slack channel via conversations.create.
 */
@ActionMapping(appKey = "slack", actionKey = "create-channel")
public class SlackCreateChannelHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SlackCreateChannelHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Slack requires a bot token");

        String channelName = str(config, "channelName");
        if (channelName == null) return ActionResult.failure("'channelName' is required");

        boolean isPrivate = "true".equalsIgnoreCase(str(config, "isPrivate"));

        logger.info("[slack] Creating channel '{}' (private={})", channelName, isPrivate);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", channelName);
            body.put("is_private", isPrivate);

            Map<String, Object> response = restClient.post()
                    .uri("https://slack.com/api/conversations.create")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "slack");
            output.put("action", "create-channel");
            output.put("ok", response != null ? response.get("ok") : false);
            if (response != null && response.containsKey("channel")) {
                var ch = (Map<String, Object>) response.get("channel");
                output.put("channelId", ch.get("id"));
                output.put("channelName", ch.get("name"));
            }
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[slack] Create channel failed: {}", e.getMessage());
            return ActionResult.failure("Slack create-channel failed: " + e.getMessage());
        }
    }

    private String resolveToken(Map<String, Object> creds) {
        if (creds == null) return null;
        String t = (String) creds.get("botToken");
        if (t == null || t.isBlank()) t = (String) creds.get("accessToken");
        return (t != null && !t.isBlank()) ? t : null;
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
