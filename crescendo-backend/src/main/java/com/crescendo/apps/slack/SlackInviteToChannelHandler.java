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
 * Invites a user to a Slack channel via conversations.invite.
 */
@ActionMapping(appKey = "slack", actionKey = "invite-to-channel")
public class SlackInviteToChannelHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SlackInviteToChannelHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Slack requires a bot token");

        String channel = str(config, "channel");
        String userId = str(config, "userId");
        if (channel == null) return ActionResult.failure("'channel' is required");
        if (userId == null) return ActionResult.failure("'userId' is required");

        logger.info("[slack] Inviting user '{}' to channel '{}'", userId, channel);

        try {
            String response = restClient.post()
                    .uri("https://slack.com/api/conversations.invite")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("channel", channel, "users", userId))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "slack");
            output.put("action", "invite-to-channel");
            output.put("channel", channel);
            output.put("userId", userId);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[slack] Invite to channel failed: {}", e.getMessage());
            return ActionResult.failure("Slack invite-to-channel failed: " + e.getMessage());
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
