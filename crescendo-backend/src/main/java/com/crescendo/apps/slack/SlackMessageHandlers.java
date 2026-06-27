package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Slack Message operations.
 */
@Component
public class SlackMessageHandlers {

    private static final String SLACK_API = SlackSupport.SLACK_API;

    // ── send ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "sendMessage")
    public ActionResult send(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channel = SlackSupport.require(config, "channel");
        String text = SlackSupport.require(config, "text");
        String blocksUi = SlackSupport.opt(config, "blocksUi", null);

        if (channel == null) return ActionResult.failure("'channel' is required");
        if (text == null && blocksUi == null) {
            return ActionResult.failure("Either 'text' or 'blocksUi' is required");
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("channel", channel);
            if (text != null) payload.put("text", text);
            
            String threadTs = SlackSupport.opt(config, "thread_ts", null);
            if (threadTs != null) payload.put("thread_ts", threadTs);
            
            String replyBroadcast = SlackSupport.opt(config, "replyBroadcast", null);
            if (replyBroadcast != null) payload.put("reply_broadcast", Boolean.parseBoolean(replyBroadcast));

            String ephemeralUserId = SlackSupport.opt(config, "ephemeralUserId", null);
            if (ephemeralUserId != null) {
                payload.put("user", ephemeralUserId);
            } else {
                String iconEmoji = SlackSupport.opt(config, "icon_emoji", null);
                if (iconEmoji != null) payload.put("icon_emoji", iconEmoji);
                
                String iconUrl = SlackSupport.opt(config, "icon_url", null);
                if (iconUrl != null) payload.put("icon_url", iconUrl);
                
                String username = SlackSupport.opt(config, "username", null);
                if (username != null) payload.put("username", username);
            }

            if (blocksUi != null) {
                try {
                    Object blocksList = SlackSupport.getMapper().readValue(blocksUi, Object.class);
                    payload.put("blocks", blocksList);
                } catch (Exception e) {
                    payload.put("blocks", blocksUi);
                }
            }

            String endpoint = (ephemeralUserId != null) ? SLACK_API + "chat.postEphemeral" : SLACK_API + "chat.postMessage";

            String response = SlackSupport.clientBuilder(context).build().post()
                    .uri(endpoint)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("channel", channel);
            output.put("response", response);
            return ActionResult.success(output);
        } catch (Exception e) {
            return ActionResult.failure("Slack sendMessage failed: " + e.getMessage());
        }
    }

    // ── sendDM ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "sendDirectMessage")
    public ActionResult sendDM(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String user = SlackSupport.require(config, "user");
        String text = SlackSupport.require(config, "text");
        String blocksUi = SlackSupport.opt(config, "blocksUi", null);

        if (user == null) return ActionResult.failure("'user' is required");
        if (text == null && blocksUi == null) {
            return ActionResult.failure("Either 'text' or 'blocksUi' is required");
        }

        try {
            // First, open a direct message channel with the user
            Map<String, Object> openPayload = Map.of("users", user);
            Map<String, Object> openResponse = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "conversations.open")
                    .body(openPayload)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            if (openResponse == null || !Boolean.TRUE.equals(openResponse.get("ok"))) {
                return ActionResult.failure("Failed to open DM channel with user " + user);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> channelObj = (Map<String, Object>) openResponse.get("channel");
            String channelId = (String) channelObj.get("id");

            // Now send the message to the opened channel
            Map<String, Object> payload = new HashMap<>();
            payload.put("channel", channelId);
            if (text != null) payload.put("text", text);

            if (blocksUi != null) {
                try {
                    Object blocksList = SlackSupport.getMapper().readValue(blocksUi, Object.class);
                    payload.put("blocks", blocksList);
                } catch (Exception e) {
                    payload.put("blocks", blocksUi);
                }
            }

            String response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "chat.postMessage")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("channel", channelId);
            output.put("response", response);
            return ActionResult.success(output);
        } catch (Exception e) {
            return ActionResult.failure("Slack sendDirectMessage failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "updateMessage")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channel = SlackSupport.require(config, "channel");
        String ts = SlackSupport.require(config, "ts");
        String text = SlackSupport.require(config, "text");
        String blocksUi = SlackSupport.opt(config, "blocksUi", null);

        if (channel == null || ts == null) return ActionResult.failure("'channel' and 'ts' are required");
        if (text == null && blocksUi == null) return ActionResult.failure("Either 'text' or 'blocksUi' is required for update");

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("channel", channel);
            payload.put("ts", ts);
            if (text != null) payload.put("text", text);

            if (blocksUi != null) {
                try {
                    Object blocksList = SlackSupport.getMapper().readValue(blocksUi, Object.class);
                    payload.put("blocks", blocksList);
                } catch (Exception e) {
                    payload.put("blocks", blocksUi);
                }
            }

            String response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "chat.update")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Slack updateMessage failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "deleteMessage")
    public ActionResult delete(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channel = SlackSupport.require(config, "channel");
        String ts = SlackSupport.require(config, "ts");

        if (channel == null || ts == null) return ActionResult.failure("'channel' and 'ts' are required");

        try {
            String response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "chat.delete")
                    .body(Map.of("channel", channel, "ts", ts))
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Slack deleteMessage failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "getMessage")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channel = SlackSupport.require(config, "channel");
        String ts = SlackSupport.require(config, "ts");

        if (channel == null || ts == null) return ActionResult.failure("'channel' and 'ts' are required");

        try {
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().get()
                    .uri(SLACK_API + "conversations.history?channel=" + channel + "&latest=" + ts + "&limit=1&inclusive=true")
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack getMessage failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "getAllMessages")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channel = SlackSupport.require(config, "channel");
        if (channel == null) return ActionResult.failure("'channel' is required");

        int limit = SlackSupport.parseIntOpt(config, "limit", 100);

        try {
            String uri = SLACK_API + "conversations.history?channel=" + channel + "&limit=" + limit;
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack getAllMessages failed: " + e.getMessage());
        }
    }

    // ── search ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "searchMessages")
    @SuppressWarnings("unchecked")
    public ActionResult search(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String query = SlackSupport.require(config, "query");
        if (query == null) return ActionResult.failure("'query' is required");

        int count = SlackSupport.parseIntOpt(config, "count", 20);

        try {
            String uri = SLACK_API + "search.messages?query=" + query + "&count=" + count;
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack searchMessages failed: " + e.getMessage());
        }
    }
}
