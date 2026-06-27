package com.crescendo.apps.discord;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Discord Channel operations.
 */
@Component
public class DiscordChannelHandlers {

    private static final String DISCORD_API = DiscordSupport.DISCORD_API;

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "createChannel")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String guildId = DiscordSupport.require(config, "guildId");
        String name = DiscordSupport.require(config, "name");

        if (guildId == null || name == null) {
            return ActionResult.failure("'guildId' and 'name' are required");
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", name);
            
            String typeStr = DiscordSupport.opt(config, "type", null);
            if (typeStr != null) {
                try { payload.put("type", Integer.parseInt(typeStr)); } catch (NumberFormatException ignored) {}
            }
            
            String topic = DiscordSupport.opt(config, "topic", null);
            if (topic != null) payload.put("topic", topic);

            String parentId = DiscordSupport.opt(config, "parentId", null);
            if (parentId != null) payload.put("parent_id", parentId);

            Map<String, Object> response = DiscordSupport.clientBuilder(context).build().post()
                    .uri(DISCORD_API + "guilds/" + guildId + "/channels")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Discord createChannel failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "deleteChannel")
    @SuppressWarnings("unchecked")
    public ActionResult delete(ActionContext context) {
        String channelId = DiscordSupport.require(context.configuration(), "channelId");
        if (channelId == null) return ActionResult.failure("'channelId' is required");

        try {
            Map<String, Object> response = DiscordSupport.clientBuilder(context).build().delete()
                    .uri(DISCORD_API + "channels/" + channelId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Discord deleteChannel failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "getChannel")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String channelId = DiscordSupport.require(context.configuration(), "channelId");
        if (channelId == null) return ActionResult.failure("'channelId' is required");

        try {
            Map<String, Object> response = DiscordSupport.clientBuilder(context).build().get()
                    .uri(DISCORD_API + "channels/" + channelId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Discord getChannel failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "getAllChannels")
// @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        String guildId = DiscordSupport.require(context.configuration(), "guildId");
        if (guildId == null) return ActionResult.failure("'guildId' is required");

        try {
            Object response = DiscordSupport.clientBuilder(context).build().get()
                    .uri(DISCORD_API + "guilds/" + guildId + "/channels")
                    .retrieve()
                    .body(Object.class); // list of channels
            return ActionResult.success(Map.of("channels", response));
        } catch (Exception e) {
            return ActionResult.failure("Discord getAllChannels failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "updateChannel")
    @SuppressWarnings("unchecked")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String channelId = DiscordSupport.require(config, "channelId");
        if (channelId == null) return ActionResult.failure("'channelId' is required");

        try {
            Map<String, Object> payload = new HashMap<>();
            
            String name = DiscordSupport.opt(config, "name", null);
            if (name != null) payload.put("name", name);
            
            String topic = DiscordSupport.opt(config, "topic", null);
            if (topic != null) payload.put("topic", topic);

            String parentId = DiscordSupport.opt(config, "parentId", null);
            if (parentId != null) payload.put("parent_id", parentId);

            Map<String, Object> response = DiscordSupport.clientBuilder(context).build().patch()
                    .uri(DISCORD_API + "channels/" + channelId)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Discord updateChannel failed: " + e.getMessage());
        }
    }
}
