package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

// import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Slack Channel operations.
 */
@Component
public class SlackChannelHandlers {

    private static final String SLACK_API = SlackSupport.SLACK_API;

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "createChannel")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String name = SlackSupport.require(config, "name");
        if (name == null) return ActionResult.failure("'name' is required");

        boolean isPrivate = Boolean.parseBoolean(SlackSupport.opt(config, "isPrivate", "false"));

        try {
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "conversations.create")
                    .body(Map.of("name", name, "is_private", isPrivate))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack createChannel failed: " + e.getMessage());
        }
    }

    // ── archive ───────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "archiveChannel")
    @SuppressWarnings("unchecked")
    public ActionResult archive(ActionContext context) {
        String channel = SlackSupport.require(context.configuration(), "channel");
        if (channel == null) return ActionResult.failure("'channel' is required");

        try {
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "conversations.archive")
                    .body(Map.of("channel", channel))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack archiveChannel failed: " + e.getMessage());
        }
    }

    // ── invite ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "inviteToChannel")
    @SuppressWarnings("unchecked")
    public ActionResult invite(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channel = SlackSupport.require(config, "channel");
        String users = SlackSupport.require(config, "users");

        if (channel == null || users == null) {
            return ActionResult.failure("'channel' and 'users' are required");
        }

        try {
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "conversations.invite")
                    .body(Map.of("channel", channel, "users", users))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack inviteToChannel failed: " + e.getMessage());
        }
    }

    // ── join ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "joinChannel")
    @SuppressWarnings("unchecked")
    public ActionResult join(ActionContext context) {
        String channel = SlackSupport.require(context.configuration(), "channel");
        if (channel == null) return ActionResult.failure("'channel' is required");

        try {
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "conversations.join")
                    .body(Map.of("channel", channel))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack joinChannel failed: " + e.getMessage());
        }
    }

    // ── setTopic ──────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "setTopic")
    @SuppressWarnings("unchecked")
    public ActionResult setTopic(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channel = SlackSupport.require(config, "channel");
        String topic = SlackSupport.require(config, "topic");

        if (channel == null || topic == null) {
            return ActionResult.failure("'channel' and 'topic' are required");
        }

        try {
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "conversations.setTopic")
                    .body(Map.of("channel", channel, "topic", topic))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack setTopic failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "getAllChannels")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        int limit = SlackSupport.parseIntOpt(config, "limit", 100);

        try {
            String types = SlackSupport.opt(config, "types", "public_channel,private_channel");
            String uri = SLACK_API + "conversations.list?limit=" + limit + "&types=" + types;
            
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack getAllChannels failed: " + e.getMessage());
        }
    }
}
