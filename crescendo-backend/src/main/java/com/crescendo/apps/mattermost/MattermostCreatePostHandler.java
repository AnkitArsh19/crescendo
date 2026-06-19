package com.crescendo.apps.mattermost;

import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import java.util.*;

@ActionMapping(appKey = "mattermost", actionKey = "create-post")
public class MattermostCreatePostHandler implements ActionHandler {
    private final ObjectMapper mapper;

    public MattermostCreatePostHandler(ObjectMapper m) {
        mapper = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String ch = val(c, "channelId");
            String msg = val(c, "message");
            if (ch.isBlank() || msg.isBlank()) {
                return ActionResult.failure("Mattermost channelId and message are required");
            }
            String res = RestClient.builder()
                    .baseUrl(trim(cred(c, "baseUrl")))
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + cred(c, "accessToken"))
                    .build()
                    .post()
                    .uri("/api/v4/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("channel_id", ch, "message", msg))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", mapper.readValue(res, Object.class), "raw", res));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost post failed: " + e.getMessage());
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
