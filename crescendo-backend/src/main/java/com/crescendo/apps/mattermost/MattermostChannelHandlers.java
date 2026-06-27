package com.crescendo.apps.mattermost;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Mattermost Channel operations.
 */
@Component
public class MattermostChannelHandlers {

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "createChannel")
// @SuppressWarnings("unchecked")
    public ActionResult createChannel(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String teamId = MattermostSupport.require(config, "teamId");
        String name = MattermostSupport.require(config, "name");
        String displayName = MattermostSupport.require(config, "displayName");
        String type = MattermostSupport.require(config, "type"); // O (public) or P (private)

        if (teamId == null || name == null || displayName == null || type == null) {
            return ActionResult.failure("'teamId', 'name', 'displayName', and 'type' are required");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("team_id", teamId);
            body.put("name", name);
            body.put("display_name", displayName);
            body.put("type", type);

            String response = MattermostSupport.clientBuilder(context).build().post()
                    .uri("/api/v4/channels")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost createChannel failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "deleteChannel")
    public ActionResult deleteChannel(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        String channelId = MattermostSupport.require(context.configuration(), "channelId");
        if (channelId == null) return ActionResult.failure("'channelId' is required");

        try {
            String response = MattermostSupport.clientBuilder(context).build().delete()
                    .uri("/api/v4/channels/" + channelId)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost deleteChannel failed: " + e.getMessage());
        }
    }

    // ── addUser ───────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "addUserToChannel")
    public ActionResult addUser(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String channelId = MattermostSupport.require(config, "channelId");
        String userId = MattermostSupport.require(config, "userId");

        if (channelId == null || userId == null) {
            return ActionResult.failure("'channelId' and 'userId' are required");
        }

        try {
            String response = MattermostSupport.clientBuilder(context).build().post()
                    .uri("/api/v4/channels/" + channelId + "/members")
                    .body(Map.of("user_id", userId))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost addUserToChannel failed: " + e.getMessage());
        }
    }

    // ── getMembers ────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "getChannelMembers")
    public ActionResult getMembers(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String channelId = MattermostSupport.require(config, "channelId");
        if (channelId == null) return ActionResult.failure("'channelId' is required");

        int page = MattermostSupport.parseIntOpt(config, "page", 0);
        int perPage = MattermostSupport.parseIntOpt(config, "perPage", 60);

        try {
            String response = MattermostSupport.clientBuilder(context).build().get()
                    .uri("/api/v4/channels/" + channelId + "/members?page=" + page + "&per_page=" + perPage)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost getChannelMembers failed: " + e.getMessage());
        }
    }

    // ── search ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "searchChannels")
    public ActionResult search(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String teamId = MattermostSupport.require(config, "teamId");
        String term = MattermostSupport.require(config, "term");

        if (teamId == null || term == null) {
            return ActionResult.failure("'teamId' and 'term' are required");
        }

        try {
            String response = MattermostSupport.clientBuilder(context).build().post()
                    .uri("/api/v4/teams/" + teamId + "/channels/search")
                    .body(Map.of("term", term))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost searchChannels failed: " + e.getMessage());
        }
    }

    // ── getStats ──────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "getChannelStats")
    public ActionResult getStats(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        String channelId = MattermostSupport.require(context.configuration(), "channelId");
        if (channelId == null) return ActionResult.failure("'channelId' is required");

        try {
            String response = MattermostSupport.clientBuilder(context).build().get()
                    .uri("/api/v4/channels/" + channelId + "/stats")
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost getChannelStats failed: " + e.getMessage());
        }
    }

    // ── restore ───────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "restoreChannel")
    public ActionResult restore(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        String channelId = MattermostSupport.require(context.configuration(), "channelId");
        if (channelId == null) return ActionResult.failure("'channelId' is required");

        try {
            String response = MattermostSupport.clientBuilder(context).build().post()
                    .uri("/api/v4/channels/" + channelId + "/restore")
                    .body(Map.of())
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost restoreChannel failed: " + e.getMessage());
        }
    }
}
