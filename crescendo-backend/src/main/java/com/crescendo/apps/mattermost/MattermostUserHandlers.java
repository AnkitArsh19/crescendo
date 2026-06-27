package com.crescendo.apps.mattermost;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grouped handler for Mattermost User operations.
 */
@Component
public class MattermostUserHandlers {

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "createUser")
// @SuppressWarnings("unchecked")
    public ActionResult createUser(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String email = MattermostSupport.require(config, "email");
        String username = MattermostSupport.require(config, "username");
        String password = MattermostSupport.require(config, "password");

        if (email == null || username == null || password == null) {
            return ActionResult.failure("'email', 'username', and 'password' are required");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("email", email);
            body.put("username", username);
            body.put("password", password);

            String firstName = MattermostSupport.opt(config, "firstName", null);
            if (firstName != null) body.put("first_name", firstName);
            
            String lastName = MattermostSupport.opt(config, "lastName", null);
            if (lastName != null) body.put("last_name", lastName);

            String response = MattermostSupport.clientBuilder(context).build().post()
                    .uri("/api/v4/users")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost createUser failed: " + e.getMessage());
        }
    }

    // ── deactivate ────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "deactivateUser")
    public ActionResult deactivateUser(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        String userId = MattermostSupport.require(context.configuration(), "userId");
        if (userId == null) return ActionResult.failure("'userId' is required");

        try {
            String response = MattermostSupport.clientBuilder(context).build().delete()
                    .uri("/api/v4/users/" + userId)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost deactivateUser failed: " + e.getMessage());
        }
    }

    // ── getByEmail ────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "getUserByEmail")
    public ActionResult getByEmail(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        String email = MattermostSupport.require(context.configuration(), "email");
        if (email == null) return ActionResult.failure("'email' is required");

        try {
            String response = MattermostSupport.clientBuilder(context).build().get()
                    .uri("/api/v4/users/email/" + email)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost getUserByEmail failed: " + e.getMessage());
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "getUserById")
    public ActionResult getById(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        String userId = MattermostSupport.require(context.configuration(), "userId");
        if (userId == null) return ActionResult.failure("'userId' is required");

        try {
            String response = MattermostSupport.clientBuilder(context).build().get()
                    .uri("/api/v4/users/" + userId)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost getUserById failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "getAllUsers")
    public ActionResult getAll(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        int page = MattermostSupport.parseIntOpt(config, "page", 0);
        int perPage = MattermostSupport.parseIntOpt(config, "perPage", 60);

        try {
            String response = MattermostSupport.clientBuilder(context).build().get()
                    .uri("/api/v4/users?page=" + page + "&per_page=" + perPage)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost getAllUsers failed: " + e.getMessage());
        }
    }

    // ── invite ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "mattermost", actionKey = "inviteUsers")
    public ActionResult invite(ActionContext context) {
        String token = MattermostSupport.resolveToken(context);
        String baseUrl = MattermostSupport.getBaseUrl(context);
        if (token == null || baseUrl == null) return MattermostSupport.missingCredentials();

        Map<String, Object> config = context.configuration();
        String teamId = MattermostSupport.require(config, "teamId");
        String emails = MattermostSupport.require(config, "emails");

        if (teamId == null || emails == null) {
            return ActionResult.failure("'teamId' and 'emails' are required");
        }

        try {
            List<String> emailList = List.of(emails.split(","));
            
            String response = MattermostSupport.clientBuilder(context).build().post()
                    .uri("/api/v4/teams/" + teamId + "/invite/email")
                    .body(emailList)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", MattermostSupport.getMapper().readValue(response, Object.class)));
        } catch (Exception e) {
            return ActionResult.failure("Mattermost inviteUsers failed: " + e.getMessage());
        }
    }
}
