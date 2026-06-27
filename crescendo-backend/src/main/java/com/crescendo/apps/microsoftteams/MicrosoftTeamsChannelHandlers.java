package com.crescendo.apps.microsoftteams;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Microsoft Teams Channel operations.
 */
@Component
public class MicrosoftTeamsChannelHandlers {

    private static final String GRAPH_API = MicrosoftTeamsSupport.GRAPH_API;

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "createChannel")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String teamId = MicrosoftTeamsSupport.require(config, "teamId");
        String displayName = MicrosoftTeamsSupport.require(config, "displayName");

        if (teamId == null || displayName == null) {
            return ActionResult.failure("'teamId' and 'displayName' are required");
        }

        try {
            Map<String, Object> channel = new HashMap<>();
            channel.put("displayName", displayName);
            
            String description = MicrosoftTeamsSupport.opt(config, "description", null);
            if (description != null) channel.put("description", description);
            
            String membershipType = MicrosoftTeamsSupport.opt(config, "membershipType", null);
            if (membershipType != null) channel.put("membershipType", membershipType);

            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/teams/" + teamId + "/channels")
                    .body(channel)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams createChannel failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "deleteChannel")
    public ActionResult delete(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String teamId = MicrosoftTeamsSupport.require(config, "teamId");
        String channelId = MicrosoftTeamsSupport.require(config, "channelId");

        if (teamId == null || channelId == null) {
            return ActionResult.failure("'teamId' and 'channelId' are required");
        }

        try {
            MicrosoftTeamsSupport.clientBuilder(context).build().delete()
                    .uri(GRAPH_API + "/teams/" + teamId + "/channels/" + channelId)
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "channelId", channelId));
        } catch (Exception e) {
            return ActionResult.failure("Teams deleteChannel failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "getChannel")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String teamId = MicrosoftTeamsSupport.require(config, "teamId");
        String channelId = MicrosoftTeamsSupport.require(config, "channelId");

        if (teamId == null || channelId == null) {
            return ActionResult.failure("'teamId' and 'channelId' are required");
        }

        try {
            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/teams/" + teamId + "/channels/" + channelId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams getChannel failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "getAllChannels")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String teamId = MicrosoftTeamsSupport.require(config, "teamId");

        if (teamId == null) {
            return ActionResult.failure("'teamId' is required");
        }

        try {
            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/teams/" + teamId + "/channels")
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams getAllChannels failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "updateChannel")
    @SuppressWarnings("unchecked")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String teamId = MicrosoftTeamsSupport.require(config, "teamId");
        String channelId = MicrosoftTeamsSupport.require(config, "channelId");

        if (teamId == null || channelId == null) {
            return ActionResult.failure("'teamId' and 'channelId' are required");
        }

        try {
            Map<String, Object> patch = new HashMap<>();
            String displayName = MicrosoftTeamsSupport.opt(config, "displayName", null);
            if (displayName != null) patch.put("displayName", displayName);
            
            String description = MicrosoftTeamsSupport.opt(config, "description", null);
            if (description != null) patch.put("description", description);

            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().patch()
                    .uri(GRAPH_API + "/teams/" + teamId + "/channels/" + channelId)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams updateChannel failed: " + e.getMessage());
        }
    }
}
