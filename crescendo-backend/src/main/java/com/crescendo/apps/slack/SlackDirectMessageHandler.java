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
 * Sends a direct message to a Slack user.
 *
 * <p>Connection credentials: {@code botToken} (xoxb-...)
 * <p>Config: {@code userId}, {@code text}
 */
@ActionMapping(appKey = "slack", actionKey = "send-direct-message")
public class SlackDirectMessageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SlackDirectMessageHandler.class);
    private static final String CONVERSATIONS_OPEN = "https://slack.com/api/conversations.open";
    private static final String CHAT_POST = "https://slack.com/api/chat.postMessage";

    private final RestClient restClient;

    public SlackDirectMessageHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String botToken = creds != null ? (String) creds.get("botToken") : null;
        if (botToken == null || botToken.isBlank()) {
            return ActionResult.failure("Slack requires a 'botToken' in connection credentials");
        }

        String userId = getRequired(config, "userId");
        String text = getRequired(config, "text");
        if (userId == null) return ActionResult.failure("'userId' is required");
        if (text == null) return ActionResult.failure("'text' is required");

        try {
            // Open DM conversation
            restClient.post()
                    .uri(CONVERSATIONS_OPEN)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("users", userId))
                    .retrieve()
                    .body(String.class);

            // Send message
            String postResponse = restClient.post()
                    .uri(CHAT_POST)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("channel", userId, "text", text))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "slack");
            output.put("userId", userId);
            output.put("response", postResponse);
            logger.info("[slack] Direct message sent successfully");
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[slack-dm] Failed to DM user {}: {}", userId, e.getMessage());
            return ActionResult.failure("Slack DM failed: " + e.getMessage());
        }
    }

    private String getRequired(Map<String, Object> config, String key) {
        Object val = config.get(key);
        return val != null ? val.toString() : null;
    }
}
