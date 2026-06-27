package com.crescendo.apps.mattermost;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Mattermost Message/Post operations.
 */
@Component
public class MattermostMessageHandlers {

    // ── post ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "createPost")
// @SuppressWarnings("unchecked")
    public ActionResult createPost(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String channelId = MattermostSupport.require(config, "channelId");
        String message = MattermostSupport.require(config, "message");

        if (channelId == null || message == null) {
            return ActionResult.failure("'channelId' and 'message' are required");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("channel_id", channelId);
            body.put("message", message);
            
            String rootId = MattermostSupport.opt(config, "rootId", null);
            if (rootId != null) body.put("root_id", rootId);

            String response = MattermostSupport.clientBuilder(context).build().post()
                    .uri("/api/v4/posts")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost createPost failed: " + e.getMessage());
        }
    }

    // ── postEphemeral ─────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "createEphemeralPost")
// @SuppressWarnings("unchecked")
    public ActionResult createEphemeralPost(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String userId = MattermostSupport.require(config, "userId");
        String channelId = MattermostSupport.require(config, "channelId");
        String message = MattermostSupport.require(config, "message");

        if (userId == null || channelId == null || message == null) {
            return ActionResult.failure("'userId', 'channelId', and 'message' are required");
        }

        try {
            Map<String, Object> post = new HashMap<>();
            post.put("channel_id", channelId);
            post.put("message", message);

            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            body.put("post", post);

            String response = MattermostSupport.clientBuilder(context).build().post()
                    .uri("/api/v4/posts/ephemeral")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost createEphemeralPost failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "deletePost")
    public ActionResult deletePost(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        String postId = MattermostSupport.require(context.configuration(), "postId");
        if (postId == null) return ActionResult.failure("'postId' is required");

        try {
            String response = MattermostSupport.clientBuilder(context).build().delete()
                    .uri("/api/v4/posts/" + postId)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost deletePost failed: " + e.getMessage());
        }
    }
}
