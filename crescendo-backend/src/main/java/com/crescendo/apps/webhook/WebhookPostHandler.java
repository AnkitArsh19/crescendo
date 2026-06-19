package com.crescendo.apps.webhook;

import com.crescendo.execution.action.*;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Sends a POST to an external webhook URL.
 */
@ActionMapping(appKey = "crescendo-webhook", actionKey = "post-webhook")
public class WebhookPostHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String url = config.get("url") != null ? config.get("url").toString() : null;
        if (url == null)
            return ActionResult.failure("'url' is required");

        String payload = config.get("payload") != null ? config.get("payload").toString() : "{}";

        try {
            String resp = restClient.post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload).retrieve().body(String.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "webhook");
            out.put("action", "post-webhook");
            out.put("url", url);
            out.put("response", resp);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Webhook POST failed: " + e.getMessage());
        }
    }
}
