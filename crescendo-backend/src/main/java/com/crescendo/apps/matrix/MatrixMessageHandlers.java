package com.crescendo.apps.matrix;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Grouped handler for Matrix Message operations.
 */
@Component
public class MatrixMessageHandlers {

    // ── sendMessage ───────────────────────────────────────────────────────────
    @ActionMapping(appKey = "matrix", actionKey = "sendMessage")
    public ActionResult sendMessage(ActionContext context) {
        String token = MatrixSupport.resolveToken(context);
        String baseUrl = MatrixSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MatrixSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String roomId = MatrixSupport.require(config, "roomId");
        String message = MatrixSupport.require(config, "message");

        if (roomId == null || message == null) {
            return ActionResult.failure("'roomId' and 'message' are required");
        }

        try {
            String txn = UUID.randomUUID().toString();
            Map<String, Object> body = new HashMap<>();
            
            String msgtype = MatrixSupport.opt(config, "msgtype", "m.text");
            body.put("msgtype", msgtype);
            body.put("body", message);

            String response = MatrixSupport.clientBuilder(context).build().put()
                    .uri("/_matrix/client/v3/rooms/" + roomId + "/send/m.room.message/" + txn)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("data", MatrixSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Matrix sendMessage failed: " + e.getMessage());
        }
    }

    // ── deleteMessage ─────────────────────────────────────────────────────────
    @ActionMapping(appKey = "matrix", actionKey = "deleteMessage")
    public ActionResult deleteMessage(ActionContext context) {
        String token = MatrixSupport.resolveToken(context);
        String baseUrl = MatrixSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MatrixSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String roomId = MatrixSupport.require(config, "roomId");
        String eventId = MatrixSupport.require(config, "eventId");

        if (roomId == null || eventId == null) {
            return ActionResult.failure("'roomId' and 'eventId' are required");
        }

        try {
            String txn = UUID.randomUUID().toString();
            Map<String, Object> body = new HashMap<>();
            
            String reason = MatrixSupport.opt(config, "reason", null);
            if (reason != null) body.put("reason", reason);

            String response = MatrixSupport.clientBuilder(context).build().put()
                    .uri("/_matrix/client/v3/rooms/" + roomId + "/redact/" + eventId + "/" + txn)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("data", MatrixSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Matrix deleteMessage failed: " + e.getMessage());
        }
    }
}
