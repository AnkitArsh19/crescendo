package com.crescendo.apps.discord;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Grouped handler for Discord Message operations.
 */
@Component
public class DiscordMessageHandlers {

    private static final String DISCORD_API = DiscordSupport.DISCORD_API;

    // ── send ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "sendMessage")
    public ActionResult send(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channelId = DiscordSupport.require(config, "channelId");
        String content = DiscordSupport.require(config, "content");
        
        if (channelId == null || content == null) {
            return ActionResult.failure("'channelId' and 'content' are required");
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("content", content);
            
            String ttsStr = DiscordSupport.opt(config, "tts", null);
            if (ttsStr != null) payload.put("tts", Boolean.parseBoolean(ttsStr));
            
            String messageReference = DiscordSupport.opt(config, "message_reference", null);
            if (messageReference != null) payload.put("message_reference", Map.of("message_id", messageReference));

            String flagsStr = DiscordSupport.opt(config, "flags", null);
            if (flagsStr != null) {
                try { payload.put("flags", Integer.parseInt(flagsStr)); } catch (NumberFormatException ignored) {}
            }

            String response = DiscordSupport.clientBuilder(context).build().post()
                    .uri(DISCORD_API + "channels/" + channelId + "/messages")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("channelId", channelId, "response", response));
        } catch (Exception e) {
            return ActionResult.failure("Discord sendMessage failed: " + e.getMessage());
        }
    }

    // ── sendEmbed ─────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "sendEmbed")
    public ActionResult sendEmbed(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channelId = DiscordSupport.require(config, "channelId");
        if (channelId == null) return ActionResult.failure("'channelId' is required");

        String title = DiscordSupport.opt(config, "title", null);
        String description = DiscordSupport.opt(config, "description", null);
        String color = DiscordSupport.opt(config, "color", null); // integer color
        String url = DiscordSupport.opt(config, "url", null);

        try {
            Map<String, Object> embed = new HashMap<>();
            if (title != null) embed.put("title", title);
            if (description != null) embed.put("description", description);
            if (url != null) embed.put("url", url);
            if (color != null) {
                try { embed.put("color", Integer.parseInt(color)); } catch (NumberFormatException ignored) {}
            }

            Map<String, Object> payload = new HashMap<>();
            String content = DiscordSupport.opt(config, "content", null);
            if (content != null) payload.put("content", content);
            
            payload.put("embeds", java.util.List.of(embed));

            String response = DiscordSupport.clientBuilder(context).build().post()
                    .uri(DISCORD_API + "channels/" + channelId + "/messages")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("channelId", channelId, "response", response));
        } catch (Exception e) {
            return ActionResult.failure("Discord sendEmbed failed: " + e.getMessage());
        }
    }

    // ── sendDM ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "sendDirectMessage")
    public ActionResult sendDM(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String userId = DiscordSupport.require(config, "userId");
        String content = DiscordSupport.require(config, "content");
        
        if (userId == null || content == null) {
            return ActionResult.failure("'userId' and 'content' are required");
        }

        try {
            // Step 1: Create DM channel
            Map<String, Object> dmPayload = Map.of("recipient_id", userId);
            Map<String, Object> dmResponse = DiscordSupport.clientBuilder(context).build().post()
                    .uri(DISCORD_API + "users/@me/channels")
                    .body(dmPayload)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
                    
            if (dmResponse == null || !dmResponse.containsKey("id")) {
                return ActionResult.failure("Failed to create DM channel with user " + userId);
            }
            String channelId = dmResponse.get("id").toString();

            // Step 2: Send message
            Map<String, Object> payload = Map.of("content", content);
            String response = DiscordSupport.clientBuilder(context).build().post()
                    .uri(DISCORD_API + "channels/" + channelId + "/messages")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("userId", userId, "channelId", channelId, "response", response));
        } catch (Exception e) {
            return ActionResult.failure("Discord sendDirectMessage failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "getMessage")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channelId = DiscordSupport.require(config, "channelId");
        String messageId = DiscordSupport.require(config, "messageId");

        if (channelId == null || messageId == null) {
            return ActionResult.failure("'channelId' and 'messageId' are required");
        }

        try {
            Map<String, Object> response = DiscordSupport.clientBuilder(context).build().get()
                    .uri(DISCORD_API + "channels/" + channelId + "/messages/" + messageId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Discord getMessage failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "getAllMessages")
// @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channelId = DiscordSupport.require(config, "channelId");
        if (channelId == null) return ActionResult.failure("'channelId' is required");

        int limit = DiscordSupport.parseIntOpt(config, "limit", 50);

        try {
            String uri = DISCORD_API + "channels/" + channelId + "/messages?limit=" + limit;
            
            String before = DiscordSupport.opt(config, "before", null);
            if (before != null) uri += "&before=" + before;

            String after = DiscordSupport.opt(config, "after", null);
            if (after != null) uri += "&after=" + after;

            Object response = DiscordSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Object.class); // list of messages
            return ActionResult.success(Map.of("messages", response));
        } catch (Exception e) {
            return ActionResult.failure("Discord getAllMessages failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "deleteMessage")
    public ActionResult delete(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channelId = DiscordSupport.require(config, "channelId");
        String messageId = DiscordSupport.require(config, "messageId");

        if (channelId == null || messageId == null) {
            return ActionResult.failure("'channelId' and 'messageId' are required");
        }

        try {
            DiscordSupport.clientBuilder(context).build().delete()
                    .uri(DISCORD_API + "channels/" + channelId + "/messages/" + messageId)
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "messageId", messageId));
        } catch (Exception e) {
            return ActionResult.failure("Discord deleteMessage failed: " + e.getMessage());
        }
    }

    // ── react ─────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "addReaction")
    public ActionResult react(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channelId = DiscordSupport.require(config, "channelId");
        String messageId = DiscordSupport.require(config, "messageId");
        String emoji = DiscordSupport.require(config, "emoji");

        if (channelId == null || messageId == null || emoji == null) {
            return ActionResult.failure("'channelId', 'messageId', and 'emoji' are required");
        }

        try {
            // URL encode emoji for Discord API (e.g., 😊 -> %F0%9F%98%8A)
            String encodedEmoji = URLEncoder.encode(emoji, StandardCharsets.UTF_8).replace("+", "%20");
            
            DiscordSupport.clientBuilder(context).build().put()
                    .uri(DISCORD_API + "channels/" + channelId + "/messages/" + messageId + "/reactions/" + encodedEmoji + "/@me")
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "messageId", messageId, "emoji", emoji));
        } catch (Exception e) {
            return ActionResult.failure("Discord addReaction failed: " + e.getMessage());
        }
    }
}
