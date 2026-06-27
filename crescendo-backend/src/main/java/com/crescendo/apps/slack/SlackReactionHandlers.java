package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grouped handler for Slack Reaction operations.
 */
@Component
public class SlackReactionHandlers {

    private static final String SLACK_API = SlackSupport.SLACK_API;

    // ── add ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "addReaction")
    @SuppressWarnings("unchecked")
    public ActionResult add(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channel = SlackSupport.require(config, "channel");
        String timestamp = SlackSupport.require(config, "timestamp");
        String name = SlackSupport.require(config, "name"); // reaction name, e.g. "thumbsup"

        if (channel == null || timestamp == null || name == null) {
            return ActionResult.failure("'channel', 'timestamp', and 'name' are required");
        }

        try {
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "reactions.add")
                    .body(Map.of("channel", channel, "timestamp", timestamp, "name", name))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack addReaction failed: " + e.getMessage());
        }
    }

    // ── remove ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "removeReaction")
    @SuppressWarnings("unchecked")
    public ActionResult remove(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channel = SlackSupport.require(config, "channel");
        String timestamp = SlackSupport.require(config, "timestamp");
        String name = SlackSupport.require(config, "name");

        if (channel == null || timestamp == null || name == null) {
            return ActionResult.failure("'channel', 'timestamp', and 'name' are required");
        }

        try {
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "reactions.remove")
                    .body(Map.of("channel", channel, "timestamp", timestamp, "name", name))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack removeReaction failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "getReaction")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channel = SlackSupport.require(config, "channel");
        String timestamp = SlackSupport.require(config, "timestamp");

        if (channel == null || timestamp == null) {
            return ActionResult.failure("'channel' and 'timestamp' are required");
        }

        try {
            String uri = SLACK_API + "reactions.get?channel=" + channel + "&timestamp=" + timestamp;
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack getReaction failed: " + e.getMessage());
        }
    }
}
