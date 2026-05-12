package com.crescendo.apps.discord;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Adds a role to a guild member via Discord REST API v10.
 */
@ActionMapping(appKey = "discord", actionKey = "add-role")
public class DiscordAddRoleHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiscordAddRoleHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String botToken = creds != null ? (String) creds.get("botToken") : null;
        if (botToken == null || botToken.isBlank()) {
            return ActionResult.failure("Discord requires a 'botToken'");
        }

        String guildId = str(config, "guildId");
        String userId = str(config, "userId");
        String roleId = str(config, "roleId");
        if (guildId == null) return ActionResult.failure("'guildId' is required");
        if (userId == null) return ActionResult.failure("'userId' is required");
        if (roleId == null) return ActionResult.failure("'roleId' is required");

        logger.info("[discord] Adding role '{}' to user '{}' in guild '{}'", roleId, userId, guildId);

        try {
            String url = "https://discord.com/api/v10/guilds/" + guildId + "/members/" + userId + "/roles/" + roleId;
            restClient.put()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
                    .retrieve()
                    .toBodilessEntity();

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "discord");
            output.put("action", "add-role");
            output.put("userId", userId);
            output.put("roleId", roleId);
            output.put("success", true);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[discord] Add role failed: {}", e.getMessage());
            return ActionResult.failure("Discord add-role failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
