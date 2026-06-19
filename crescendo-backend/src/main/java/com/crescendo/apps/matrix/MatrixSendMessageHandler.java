package com.crescendo.apps.matrix;

import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import java.util.*;

@ActionMapping(appKey = "matrix", actionKey = "send-message")
public class MatrixSendMessageHandler implements ActionHandler {
    private final ObjectMapper mapper;

    public MatrixSendMessageHandler(ObjectMapper m) {
        mapper = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String room = val(c, "roomId");
            String msg = val(c, "message");
            if (room.isBlank() || msg.isBlank()) {
                return ActionResult.failure("Matrix roomId and message are required");
            }
            String txn = UUID.randomUUID().toString();
            String res = RestClient.builder()
                    .baseUrl(trim(cred(c, "baseUrl")))
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + cred(c, "accessToken"))
                    .build()
                    .put()
                    .uri("/_matrix/client/v3/rooms/{room}/send/m.room.message/{txn}", room, txn)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("msgtype", "m.text", "body", msg))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", mapper.readValue(res, Object.class), "raw", res));
        } catch (Exception e) {
            return ActionResult.failure("Matrix send failed: " + e.getMessage());
        }
    }

    String val(ActionContext c, String k) {
        Object v = c.configuration().get(k);
        return v == null ? "" : String.valueOf(v);
    }

    String cred(ActionContext c, String k) {
        Object v = c.credentials() != null ? c.credentials().get(k) : null;
        return v == null ? "" : String.valueOf(v);
    }

    String trim(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
