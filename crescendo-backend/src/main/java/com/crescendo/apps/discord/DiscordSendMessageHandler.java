package com.crescendo.apps.discord;

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
 * Sends a message to a Discord channel via the Discord REST API v10.
 *
 * <p>Connection credentials: {@code botToken}
 * <p>Config: {@code channelId}, {@code content}
 */
@ActionMapping(appKey = "discord", actionKey = "send-message")
public class DiscordSendMessageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiscordSendMessageHandler.class);

    private final RestClient restClient;

    public DiscordSendMessageHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String botToken = creds != null ? (String) creds.get("botToken") : null;
        if (botToken == null || botToken.isBlank()) {
            logger.warn("[discord] send-message: missing botToken");
            return ActionResult.failure("Discord requires a 'botToken' in connection credentials");
        }

        String channelId = getRequired(config, "channelId");
        String content = getRequired(config, "content");
        if (channelId == null) return ActionResult.failure("'channelId' is required");
        if (content == null) return ActionResult.failure("'content' is required");

        logger.info("[discord] Sending message to channel='{}'", channelId);

        try {
            String url = "https://discord.com/api/v10/channels/" + channelId + "/messages";

            String response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("content", content))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "discord");
            output.put("channelId", channelId);
            output.put("response", response);
            logger.info("[discord] Message sent successfully to channel='{}'", channelId);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[discord] Failed to send message to channel {}: {}", channelId, e.getMessage());
            return ActionResult.failure("Discord send failed: " + e.getMessage());
        }
    }

    private String getRequired(Map<String, Object> config, String key) {
        Object val = config.get(key);
        return val != null ? val.toString() : null;
    }
}
