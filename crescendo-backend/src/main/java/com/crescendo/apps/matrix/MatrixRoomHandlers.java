package com.crescendo.apps.matrix;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Matrix Room operations.
 */
@Component
public class MatrixRoomHandlers {

    // ── createRoom ────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "matrix", actionKey = "createRoom")
    public ActionResult createRoom(ActionContext context) {
        String token = MatrixSupport.resolveToken(context);
        String baseUrl = MatrixSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MatrixSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        
        try {
            Map<String, Object> body = new HashMap<>();
            
            String name = MatrixSupport.opt(config, "name", null);
            if (name != null) body.put("name", name);
            
            String roomAliasName = MatrixSupport.opt(config, "roomAliasName", null);
            if (roomAliasName != null) body.put("room_alias_name", roomAliasName);
            
            String topic = MatrixSupport.opt(config, "topic", null);
            if (topic != null) body.put("topic", topic);
            
            String visibility = MatrixSupport.opt(config, "visibility", "private");
            body.put("visibility", visibility);

            String response = MatrixSupport.clientBuilder(context).build().post()
                    .uri("/_matrix/client/v3/createRoom")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("data", MatrixSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Matrix createRoom failed: " + e.getMessage());
        }
    }

    // ── invite ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "matrix", actionKey = "inviteToRoom")
    public ActionResult invite(ActionContext context) {
        String token = MatrixSupport.resolveToken(context);
        String baseUrl = MatrixSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MatrixSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String roomId = MatrixSupport.require(config, "roomId");
        String userId = MatrixSupport.require(config, "userId");

        if (roomId == null || userId == null) {
            return ActionResult.failure("'roomId' and 'userId' are required");
        }
        
        try {
            String response = MatrixSupport.clientBuilder(context).build().post()
                    .uri("/_matrix/client/v3/rooms/" + roomId + "/invite")
                    .body(Map.of("user_id", userId))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MatrixSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Matrix inviteToRoom failed: " + e.getMessage());
        }
    }

    // ── join ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "matrix", actionKey = "joinRoom")
    public ActionResult join(ActionContext context) {
        String token = MatrixSupport.resolveToken(context);
        String baseUrl = MatrixSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MatrixSupport.missingCredentials();

        String roomIdOrAlias = MatrixSupport.require(context.configuration(), "roomIdOrAlias");
        if (roomIdOrAlias == null) return ActionResult.failure("'roomIdOrAlias' is required");
        
        try {
            String response = MatrixSupport.clientBuilder(context).build().post()
                    .uri("/_matrix/client/v3/join/" + roomIdOrAlias)
                    .body(Map.of())
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MatrixSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Matrix joinRoom failed: " + e.getMessage());
        }
    }

    // ── leave ─────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "matrix", actionKey = "leaveRoom")
    public ActionResult leave(ActionContext context) {
        String token = MatrixSupport.resolveToken(context);
        String baseUrl = MatrixSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MatrixSupport.missingCredentials();

        String roomId = MatrixSupport.require(context.configuration(), "roomId");
        if (roomId == null) return ActionResult.failure("'roomId' is required");
        
        try {
            String response = MatrixSupport.clientBuilder(context).build().post()
                    .uri("/_matrix/client/v3/rooms/" + roomId + "/leave")
                    .body(Map.of())
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MatrixSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Matrix leaveRoom failed: " + e.getMessage());
        }
    }

    // ── kick ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "matrix", actionKey = "kickFromRoom")
    public ActionResult kick(ActionContext context) {
        String token = MatrixSupport.resolveToken(context);
        String baseUrl = MatrixSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MatrixSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String roomId = MatrixSupport.require(config, "roomId");
        String userId = MatrixSupport.require(config, "userId");

        if (roomId == null || userId == null) {
            return ActionResult.failure("'roomId' and 'userId' are required");
        }
        
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            
            String reason = MatrixSupport.opt(config, "reason", null);
            if (reason != null) body.put("reason", reason);

            String response = MatrixSupport.clientBuilder(context).build().post()
                    .uri("/_matrix/client/v3/rooms/" + roomId + "/kick")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MatrixSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Matrix kickFromRoom failed: " + e.getMessage());
        }
    }
}
