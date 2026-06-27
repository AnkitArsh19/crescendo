package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grouped handler for Slack User operations.
 */
@Component
public class SlackUserHandlers {

    private static final String SLACK_API = SlackSupport.SLACK_API;

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "getUser")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String user = SlackSupport.require(context.configuration(), "user");
        if (user == null) return ActionResult.failure("'user' is required");

        try {
            String uri = SLACK_API + "users.info?user=" + user;
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack getUser failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "getAllUsers")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        int limit = SlackSupport.parseIntOpt(context.configuration(), "limit", 100);

        try {
            String uri = SLACK_API + "users.list?limit=" + limit;
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack getAllUsers failed: " + e.getMessage());
        }
    }

    // ── getPresence ───────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "getUserPresence")
    @SuppressWarnings("unchecked")
    public ActionResult getPresence(ActionContext context) {
        String user = SlackSupport.require(context.configuration(), "user");
        if (user == null) return ActionResult.failure("'user' is required");

        try {
            String uri = SLACK_API + "users.getPresence?user=" + user;
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack getUserPresence failed: " + e.getMessage());
        }
    }

    // ── updateProfile ─────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "updateUserProfile")
    @SuppressWarnings("unchecked")
    public ActionResult updateProfile(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String profileJson = SlackSupport.require(config, "profile");
        if (profileJson == null) return ActionResult.failure("'profile' is required");

        try {
            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "users.profile.set")
                    .body(Map.of("profile", profileJson))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack updateUserProfile failed: " + e.getMessage());
        }
    }
}
