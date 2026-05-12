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
import java.util.List;
import java.util.Map;

/**
 * Sends a rich embed message to a Discord channel via Discord REST API v10.
 */
@ActionMapping(appKey = "discord", actionKey = "send-embed")
public class DiscordSendEmbedHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiscordSendEmbedHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String botToken = creds != null ? (String) creds.get("botToken") : null;
        if (botToken == null || botToken.isBlank()) {
            return ActionResult.failure("Discord requires a 'botToken'");
        }

        String channelId = str(config, "channelId");
        String title = str(config, "title");
        if (channelId == null) return ActionResult.failure("'channelId' is required");
        if (title == null) return ActionResult.failure("'title' is required");

        String description = str(config, "description");
        String color = str(config, "color");

        logger.info("[discord] Sending embed to channel '{}'", channelId);

        try {
            Map<String, Object> embed = new HashMap<>();
            embed.put("title", title);
            if (description != null) embed.put("description", description);
            if (color != null) {
                try {
                    embed.put("color", Integer.parseInt(color.replace("#", ""), 16));
                } catch (NumberFormatException ignored) {}
            }

            String url = "https://discord.com/api/v10/channels/" + channelId + "/messages";
            String response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("embeds", List.of(embed)))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "discord");
            output.put("action", "send-embed");
            output.put("channelId", channelId);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[discord] Send embed failed: {}", e.getMessage());
            return ActionResult.failure("Discord send-embed failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
