package com.crescendo.apps.microsoftteams;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

// import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Microsoft Teams Channel Message operations.
 */
@Component
public class MicrosoftTeamsChannelMessageHandlers {

    private static final String GRAPH_API = MicrosoftTeamsSupport.GRAPH_API;

    // ── send ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "sendChannelMessage")
    @SuppressWarnings("unchecked")
    public ActionResult send(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String teamId = MicrosoftTeamsSupport.require(config, "teamId");
        String channelId = MicrosoftTeamsSupport.require(config, "channelId");
        String message = MicrosoftTeamsSupport.require(config, "message");

        if (teamId == null || channelId == null || message == null) {
            return ActionResult.failure("'teamId', 'channelId', and 'message' are required");
        }

        try {
            String contentType = MicrosoftTeamsSupport.opt(config, "contentType", "html");
            
            Map<String, Object> body = Map.of(
                    "body", Map.of(
                            "contentType", contentType,
                            "content", message
                    )
            );

            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/teams/" + teamId + "/channels/" + channelId + "/messages")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams sendChannelMessage failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "getAllChannelMessages")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String teamId = MicrosoftTeamsSupport.require(config, "teamId");
        String channelId = MicrosoftTeamsSupport.require(config, "channelId");

        if (teamId == null || channelId == null) {
            return ActionResult.failure("'teamId' and 'channelId' are required");
        }

        try {
            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/teams/" + teamId + "/channels/" + channelId + "/messages")
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams getAllChannelMessages failed: " + e.getMessage());
        }
    }
}
