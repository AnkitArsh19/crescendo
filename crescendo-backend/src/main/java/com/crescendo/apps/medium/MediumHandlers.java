package com.crescendo.apps.medium;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Medium operations.
 */
@Component
public class MediumHandlers {

    private static final tools.jackson.databind.ObjectMapper m = new tools.jackson.databind.ObjectMapper();

    @ActionMapping(appKey = "medium", actionKey = "createPost")
    @SuppressWarnings("unchecked")
    public ActionResult createPost(ActionContext c) {
        try {
            var client = SimpleApiSupport.bearer("https://api.medium.com/v1", SimpleApiSupport.cred(c, "integrationToken"));
            String me = client.get().uri("/me").retrieve().body(String.class);
            Object data = m.readValue(me, Object.class);
            String userId = String.valueOf(((Map<String, Object>) ((Map<String, Object>) data).get("data")).get("id"));
            
            String contentFormat = SimpleApiSupport.cfg(c, "contentFormat");
            if (contentFormat == null || contentFormat.isBlank()) contentFormat = "markdown";
            
            String publishStatus = SimpleApiSupport.cfg(c, "publishStatus");
            if (publishStatus == null || publishStatus.isBlank()) publishStatus = "draft";

            Map<String, Object> body = new HashMap<>();
            body.put("title", SimpleApiSupport.cfg(c, "title"));
            body.put("contentFormat", contentFormat);
            body.put("content", SimpleApiSupport.cfg(c, "content"));
            body.put("publishStatus", publishStatus);

            String tags = SimpleApiSupport.cfg(c, "tags");
            if (tags != null && !tags.isBlank()) {
                body.put("tags", java.util.Arrays.asList(tags.split(",")));
            }
            
            String canonicalUrl = SimpleApiSupport.cfg(c, "canonicalUrl");
            if (canonicalUrl != null && !canonicalUrl.isBlank()) body.put("canonicalUrl", canonicalUrl);

            String res = client.post()
                    .uri("/users/{id}/posts", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Medium create post failed: " + e.getMessage());
        }
    }
}
