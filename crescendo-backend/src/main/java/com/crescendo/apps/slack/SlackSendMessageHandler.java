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
 * Posts a message to a Slack channel via chat.postMessage.
 *
 * <p>Connection credentials: {@code botToken} (xoxb-...)
 * <p>Config: {@code channel}, {@code text}
 */
@ActionMapping(appKey = "slack", actionKey = "send-message")
public class SlackSendMessageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SlackSendMessageHandler.class);
    private static final String SLACK_API = "https://slack.com/api/chat.postMessage";

    private final RestClient restClient;

    public SlackSendMessageHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String botToken = creds != null ? (String) creds.get("botToken") : null;
        if (botToken == null || botToken.isBlank()) {
            logger.warn("[slack] send-message: missing botToken");
            return ActionResult.failure("Slack requires a 'botToken' in connection credentials");
        }

        String channel = getRequired(config, "channel");
        String text = getRequired(config, "text");
        if (channel == null) return ActionResult.failure("'channel' is required");
        if (text == null) return ActionResult.failure("'text' is required");

        logger.info("[slack] Sending message to channel='{}'", channel);

        try {
            String response = restClient.post()
                    .uri(SLACK_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("channel", channel, "text", text))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "slack");
            output.put("channel", channel);
            output.put("response", response);
            logger.info("[slack] Message sent successfully to channel='{}'", channel);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[slack] Failed to send message to {}: {}", channel, e.getMessage());
            return ActionResult.failure("Slack send failed: " + e.getMessage());
        }
    }

    private String getRequired(Map<String, Object> config, String key) {
        Object val = config.get(key);
        return val != null ? val.toString() : null;
    }
}
