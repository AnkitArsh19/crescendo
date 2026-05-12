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
 * Creates a new text channel in a Discord guild via REST API v10.
 */
@ActionMapping(appKey = "discord", actionKey = "create-channel")
public class DiscordCreateChannelHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiscordCreateChannelHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String botToken = creds != null ? (String) creds.get("botToken") : null;
        if (botToken == null || botToken.isBlank()) {
            return ActionResult.failure("Discord requires a 'botToken'");
        }

        String guildId = str(config, "guildId");
        String channelName = str(config, "channelName");
        if (guildId == null) return ActionResult.failure("'guildId' is required");
        if (channelName == null) return ActionResult.failure("'channelName' is required");

        String topic = str(config, "topic");
        logger.info("[discord] Creating channel '{}' in guild '{}'", channelName, guildId);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", channelName);
            body.put("type", 0); // GUILD_TEXT
            if (topic != null) body.put("topic", topic);

            String url = "https://discord.com/api/v10/guilds/" + guildId + "/channels";
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "discord");
            output.put("action", "create-channel");
            output.put("channelId", response != null ? response.get("id") : null);
            output.put("channelName", channelName);
            output.put("guildId", guildId);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[discord] Create channel failed: {}", e.getMessage());
            return ActionResult.failure("Discord create-channel failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
