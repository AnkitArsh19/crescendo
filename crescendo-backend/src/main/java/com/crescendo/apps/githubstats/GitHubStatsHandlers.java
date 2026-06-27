package com.crescendo.apps.githubstats;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class GitHubStatsHandlers {

    private static final String GITHUB_API_BASE = "https://api.github.com";

    @ActionMapping(appKey = "github-stats", actionKey = "get-profile-stats")
    public Object getStats(ActionContext context) throws Exception {
        String username = context.configuration().get("username") != null ? context.configuration().get("username").toString() : "";
        if (username.isBlank()) {
            return ActionResult.failure("GitHub username is required");
        }

        try {
            String response = RestClient.create(GITHUB_API_BASE)
                    .get()
                    .uri("/users/{username}", username)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Crescendo-App")
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to fetch GitHub profile stats: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "github-stats", actionKey = "get-repos")
    public Object getRepos(ActionContext context) throws Exception {
        String username = context.configuration().get("username") != null ? context.configuration().get("username").toString() : "";
        if (username.isBlank()) {
            return ActionResult.failure("GitHub username is required");
        }

        String sort = context.configuration().get("sort") != null ? context.configuration().get("sort").toString() : "updated";

        try {
            String response = RestClient.create(GITHUB_API_BASE)
                    .get()
                    .uri("/users/{username}/repos?sort={sort}&per_page=100", username, sort)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Crescendo-App")
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to fetch GitHub repositories: " + e.getMessage());
        }
    }
}
