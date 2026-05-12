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
 * Sets or updates a channel's topic via conversations.setTopic.
 */
@ActionMapping(appKey = "slack", actionKey = "set-channel-topic")
public class SlackSetTopicHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SlackSetTopicHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Slack requires a bot token");

        String channel = str(config, "channel");
        String topic = str(config, "topic");
        if (channel == null) return ActionResult.failure("'channel' is required");
        if (topic == null) return ActionResult.failure("'topic' is required");

        logger.info("[slack] Setting topic for channel '{}'", channel);

        try {
            String response = restClient.post()
                    .uri("https://slack.com/api/conversations.setTopic")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("channel", channel, "topic", topic))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "slack");
            output.put("action", "set-channel-topic");
            output.put("channel", channel);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[slack] Set topic failed: {}", e.getMessage());
            return ActionResult.failure("Slack set-topic failed: " + e.getMessage());
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
