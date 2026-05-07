package com.crescendo.apps.githubstats;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "github-stats", actionKey = "get-profile-stats")
public class GitHubStatsGetProfileHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GitHubStatsGetProfileHandler.class);
    private static final String GITHUB_API = "https://api.github.com";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();

        String username = config.get("username") != null ? config.get("username").toString() : null;
        if (username == null || username.isBlank()) {
            return ActionResult.failure("'username' is required");
        }

        try {
            String response = RestClient.create()
                    .get()
                    .uri(GITHUB_API + "/users/{username}", username)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[github-stats] Profile fetched for user={}", username);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[github-profile-stats] Get profile failed", e);
            return ActionResult.failure("Failed to fetch GitHub profile: " + e.getMessage());
        }
    }
}
