package com.crescendo.apps.mattermost;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grouped handler for Mattermost Reaction operations.
 */
@Component
public class MattermostReactionHandlers {

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "createReaction")
    public ActionResult createReaction(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String userId = MattermostSupport.require(config, "userId");
        String postId = MattermostSupport.require(config, "postId");
        String emojiName = MattermostSupport.require(config, "emojiName");

        if (userId == null || postId == null || emojiName == null) {
            return ActionResult.failure("'userId', 'postId', and 'emojiName' are required");
        }

        try {
            String response = MattermostSupport.clientBuilder(context).build().post()
                    .uri("/api/v4/reactions")
                    .body(Map.of("user_id", userId, "post_id", postId, "emoji_name", emojiName))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost createReaction failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "deleteReaction")
    public ActionResult deleteReaction(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String userId = MattermostSupport.require(config, "userId");
        String postId = MattermostSupport.require(config, "postId");
        String emojiName = MattermostSupport.require(config, "emojiName");

        if (userId == null || postId == null || emojiName == null) {
            return ActionResult.failure("'userId', 'postId', and 'emojiName' are required");
        }

        try {
            String uri = "/api/v4/users/" + userId + "/posts/" + postId + "/reactions/" + emojiName;
            String response = MattermostSupport.clientBuilder(context).build().delete()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost deleteReaction failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "getAllReactions")
    public ActionResult getAllReactions(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        String postId = MattermostSupport.require(context.configuration(), "postId");
        if (postId == null) return ActionResult.failure("'postId' is required");

        try {
            String response = MattermostSupport.clientBuilder(context).build().get()
                    .uri("/api/v4/posts/" + postId + "/reactions")
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost getAllReactions failed: " + e.getMessage());
        }
    }
}
