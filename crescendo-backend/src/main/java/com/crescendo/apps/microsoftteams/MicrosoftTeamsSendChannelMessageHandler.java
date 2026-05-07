package com.crescendo.apps.microsoftteams;

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

@ActionMapping(appKey = "microsoft-teams", actionKey = "send-channel-message")
public class MicrosoftTeamsSendChannelMessageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftTeamsSendChannelMessageHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = asString(creds != null ? creds.get("accessToken") : null);
        if (accessToken == null || accessToken.isBlank()) {
            logger.warn("[microsoft-teams] send-channel-message: missing accessToken");
            return ActionResult.failure("Microsoft Teams requires 'accessToken' in connection credentials");
        }

        String teamId = asString(config.get("teamId"));
        String channelId = asString(config.get("channelId"));
        String message = asString(config.get("message"));

        if (teamId == null || teamId.isBlank()) return ActionResult.failure("'teamId' is required");
        if (channelId == null || channelId.isBlank()) return ActionResult.failure("'channelId' is required");
        if (message == null || message.isBlank()) return ActionResult.failure("'message' is required");

        logger.info("[microsoft-teams] Sending message to team='{}', channel='{}'", teamId, channelId);

        try {
            String response = RestClient.create()
                    .post()
                    .uri(GRAPH_API + "/teams/" + teamId + "/channels/" + channelId + "/messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("body", Map.of("content", message)))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-teams");
            output.put("response", response);
            logger.info("[microsoft-teams] Message sent successfully to team='{}', channel='{}'", teamId, channelId);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[microsoft-teams] Send channel message failed", e);
            return ActionResult.failure("Microsoft Teams send channel message failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
