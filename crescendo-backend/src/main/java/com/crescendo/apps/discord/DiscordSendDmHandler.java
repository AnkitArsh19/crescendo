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
 * Sends a direct message to a Discord user by creating a DM channel first.
 */
@ActionMapping(appKey = "discord", actionKey = "send-dm")
public class DiscordSendDmHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiscordSendDmHandler.class);
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

        String userId = str(config, "userId");
        String content = str(config, "content");
        if (userId == null) return ActionResult.failure("'userId' is required");
        if (content == null) return ActionResult.failure("'content' is required");

        logger.info("[discord] Sending DM to user '{}'", userId);

        try {
            // Step 1: Create DM channel
            Map<String, Object> dmChannel = restClient.post()
                    .uri("https://discord.com/api/v10/users/@me/channels")
                    .header(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("recipient_id", userId))
                    .retrieve()
                    .body(Map.class);

            String dmChannelId = (String) dmChannel.get("id");

            // Step 2: Send message in the DM channel
            String response = restClient.post()
                    .uri("https://discord.com/api/v10/channels/" + dmChannelId + "/messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("content", content))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "discord");
            output.put("action", "send-dm");
            output.put("userId", userId);
            output.put("dmChannelId", dmChannelId);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[discord] Send DM failed: {}", e.getMessage());
            return ActionResult.failure("Discord send-dm failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
