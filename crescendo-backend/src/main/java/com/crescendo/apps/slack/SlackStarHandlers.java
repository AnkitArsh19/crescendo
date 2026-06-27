package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grouped handler for Slack Star operations.
 */
@Component
public class SlackStarHandlers {

    private static final String SLACK_API = SlackSupport.SLACK_API;

    // ── add ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "addStar")
    @SuppressWarnings("unchecked")
    public ActionResult add(ActionContext context) {
        Map<String, Object> config = context.configuration();
        
        // Stars can be added to messages (channel+timestamp) or files (fileId)
        String channel = SlackSupport.opt(config, "channel", null);
        String timestamp = SlackSupport.opt(config, "timestamp", null);
        String fileId = SlackSupport.opt(config, "fileId", null);
        
        if (fileId == null && (channel == null || timestamp == null)) {
            return ActionResult.failure("Either 'fileId' or ('channel' and 'timestamp') must be provided");
        }

        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            if (fileId != null) {
                payload.put("file", fileId);
            } else {
                payload.put("channel", channel);
                payload.put("timestamp", timestamp);
            }

            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "stars.add")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack addStar failed: " + e.getMessage());
        }
    }

    // ── remove ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "removeStar")
    @SuppressWarnings("unchecked")
    public ActionResult remove(ActionContext context) {
        Map<String, Object> config = context.configuration();
        
        String channel = SlackSupport.opt(config, "channel", null);
        String timestamp = SlackSupport.opt(config, "timestamp", null);
        String fileId = SlackSupport.opt(config, "fileId", null);
        
        if (fileId == null && (channel == null || timestamp == null)) {
            return ActionResult.failure("Either 'fileId' or ('channel' and 'timestamp') must be provided");
        }

        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            if (fileId != null) {
                payload.put("file", fileId);
            } else {
                payload.put("channel", channel);
                payload.put("timestamp", timestamp);
            }

            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "stars.remove")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack removeStar failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "getAllStars")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        int limit = SlackSupport.parseIntOpt(context.configuration(), "limit", 100);

        try {
            String uri = SLACK_API + "stars.list?limit=" + limit;
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack getAllStars failed: " + e.getMessage());
        }
    }
}
