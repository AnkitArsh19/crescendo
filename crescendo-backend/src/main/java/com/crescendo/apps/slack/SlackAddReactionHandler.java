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
 * Adds an emoji reaction to a Slack message via reactions.add.
 */
@ActionMapping(appKey = "slack", actionKey = "add-reaction")
public class SlackAddReactionHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SlackAddReactionHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Slack requires a bot token");

        String channel = str(config, "channel");
        String timestamp = str(config, "timestamp");
        String emoji = str(config, "emoji");
        if (channel == null) return ActionResult.failure("'channel' is required");
        if (timestamp == null) return ActionResult.failure("'timestamp' is required");
        if (emoji == null) return ActionResult.failure("'emoji' is required");

        logger.info("[slack] Adding reaction '{}' to message in channel '{}'", emoji, channel);

        try {
            String response = restClient.post()
                    .uri("https://slack.com/api/reactions.add")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("channel", channel, "timestamp", timestamp, "name", emoji))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "slack");
            output.put("action", "add-reaction");
            output.put("channel", channel);
            output.put("emoji", emoji);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[slack] Add reaction failed: {}", e.getMessage());
            return ActionResult.failure("Slack add-reaction failed: " + e.getMessage());
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
