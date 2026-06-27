package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Slack UserGroup operations.
 */
@Component
public class SlackUserGroupHandlers {

    private static final String SLACK_API = SlackSupport.SLACK_API;

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "createUserGroup")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String name = SlackSupport.require(config, "name");
        if (name == null) return ActionResult.failure("'name' is required");

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", name);
            
            String handle = SlackSupport.opt(config, "handle", null);
            if (handle != null) payload.put("handle", handle);

            String description = SlackSupport.opt(config, "description", null);
            if (description != null) payload.put("description", description);

            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "usergroups.create")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack createUserGroup failed: " + e.getMessage());
        }
    }

    // ── disable ───────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "disableUserGroup")
    @SuppressWarnings("unchecked")
    public ActionResult disable(ActionContext context) {
        String usergroupId = SlackSupport.require(context.configuration(), "usergroup");
        if (usergroupId == null) return ActionResult.failure("'usergroup' is required");

        try {
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "usergroups.disable")
                    .body(Map.of("usergroup", usergroupId))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack disableUserGroup failed: " + e.getMessage());
        }
    }

    // ── enable ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "enableUserGroup")
    @SuppressWarnings("unchecked")
    public ActionResult enable(ActionContext context) {
        String usergroupId = SlackSupport.require(context.configuration(), "usergroup");
        if (usergroupId == null) return ActionResult.failure("'usergroup' is required");

        try {
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "usergroups.enable")
                    .body(Map.of("usergroup", usergroupId))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack enableUserGroup failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "getAllUserGroups")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        boolean includeUsers = Boolean.parseBoolean(SlackSupport.opt(context.configuration(), "includeUsers", "false"));
        boolean includeCount = Boolean.parseBoolean(SlackSupport.opt(context.configuration(), "includeCount", "false"));

        try {
            String uri = SLACK_API + "usergroups.list?include_users=" + includeUsers + "&include_count=" + includeCount;
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack getAllUserGroups failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "updateUserGroup")
    @SuppressWarnings("unchecked")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String usergroupId = SlackSupport.require(config, "usergroup");
        if (usergroupId == null) return ActionResult.failure("'usergroup' is required");

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("usergroup", usergroupId);
            
            String name = SlackSupport.opt(config, "name", null);
            if (name != null) payload.put("name", name);
            
            String handle = SlackSupport.opt(config, "handle", null);
            if (handle != null) payload.put("handle", handle);

            String description = SlackSupport.opt(config, "description", null);
            if (description != null) payload.put("description", description);

            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "usergroups.update")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack updateUserGroup failed: " + e.getMessage());
        }
    }
}
