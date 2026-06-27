package com.crescendo.apps.discord;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grouped handler for Discord Member operations.
 */
@Component
public class DiscordMemberHandlers {

    private static final String DISCORD_API = DiscordSupport.DISCORD_API;

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "getAllMembers")
// @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String guildId = DiscordSupport.require(config, "guildId");
        if (guildId == null) return ActionResult.failure("'guildId' is required");

        int limit = DiscordSupport.parseIntOpt(config, "limit", 100);

        try {
            String uri = DISCORD_API + "guilds/" + guildId + "/members?limit=" + limit;
            
            String after = DiscordSupport.opt(config, "after", null);
            if (after != null) uri += "&after=" + after;

            Object response = DiscordSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Object.class); // list of members
            return ActionResult.success(Map.of("members", response));
        } catch (Exception e) {
            return ActionResult.failure("Discord getAllMembers failed: " + e.getMessage());
        }
    }

    // ── addRole ───────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "addRole")
    public ActionResult addRole(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String guildId = DiscordSupport.require(config, "guildId");
        String userId = DiscordSupport.require(config, "userId");
        String roleId = DiscordSupport.require(config, "roleId");

        if (guildId == null || userId == null || roleId == null) {
            return ActionResult.failure("'guildId', 'userId', and 'roleId' are required");
        }

        try {
            DiscordSupport.clientBuilder(context).build().put()
                    .uri(DISCORD_API + "guilds/" + guildId + "/members/" + userId + "/roles/" + roleId)
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "userId", userId, "roleId", roleId));
        } catch (Exception e) {
            return ActionResult.failure("Discord addRole failed: " + e.getMessage());
        }
    }

    // ── removeRole ────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "discord", actionKey = "removeRole")
    public ActionResult removeRole(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String guildId = DiscordSupport.require(config, "guildId");
        String userId = DiscordSupport.require(config, "userId");
        String roleId = DiscordSupport.require(config, "roleId");

        if (guildId == null || userId == null || roleId == null) {
            return ActionResult.failure("'guildId', 'userId', and 'roleId' are required");
        }

        try {
            DiscordSupport.clientBuilder(context).build().delete()
                    .uri(DISCORD_API + "guilds/" + guildId + "/members/" + userId + "/roles/" + roleId)
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "userId", userId, "roleId", roleId));
        } catch (Exception e) {
            return ActionResult.failure("Discord removeRole failed: " + e.getMessage());
        }
    }
}
