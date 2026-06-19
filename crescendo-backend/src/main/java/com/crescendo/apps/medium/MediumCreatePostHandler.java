package com.crescendo.apps.medium;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import java.util.*;

@ActionMapping(appKey = "medium", actionKey = "create-post")
public class MediumCreatePostHandler implements ActionHandler {
    private final ObjectMapper m;

    public MediumCreatePostHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            var client = SimpleApiSupport.bearer("https://api.medium.com/v1", SimpleApiSupport.cred(c, "integrationToken"));
            String me = client.get().uri("/me").retrieve().body(String.class);
            Object data = m.readValue(me, Object.class);
            String userId = String.valueOf(((Map<?, ?>) ((Map<?, ?>) data).get("data")).get("id"));
            String res = client.post()
                    .uri("/users/{id}/posts", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "title", SimpleApiSupport.cfg(c, "title"),
                            "contentFormat", SimpleApiSupport.cfg(c, "contentFormat").isBlank() ? "markdown" : SimpleApiSupport.cfg(c, "contentFormat"),
                            "content", SimpleApiSupport.cfg(c, "content"),
                            "publishStatus", SimpleApiSupport.cfg(c, "publishStatus").isBlank() ? "draft" : SimpleApiSupport.cfg(c, "publishStatus")
                    ))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Medium create post failed: " + e.getMessage());
        }
    }
}
