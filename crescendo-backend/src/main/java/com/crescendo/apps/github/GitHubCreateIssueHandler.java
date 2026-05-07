package com.crescendo.apps.github;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a GitHub issue via the GitHub REST API.
 *
 * <p>Connection credentials: {@code accessToken}
 * <p>Config: {@code owner}, {@code repo}, {@code title}, {@code body}
 */
@ActionMapping(appKey = "github", actionKey = "create-issue")
public class GitHubCreateIssueHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GitHubCreateIssueHandler.class);
    private static final String GITHUB_API = "https://api.github.com";

    private final RestClient restClient;

    public GitHubCreateIssueHandler() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            logger.warn("[github] create-issue: missing accessToken");
            return ActionResult.failure("GitHub requires an 'accessToken' in connection credentials");
        }

        String owner = getRequired(config, "owner");
        String repo = getRequired(config, "repo");
        String title = getRequired(config, "title");
        if (owner == null) return ActionResult.failure("'owner' is required");
        if (repo == null) return ActionResult.failure("'repo' is required");
        if (title == null) return ActionResult.failure("'title' is required");

        String body = config.get("body") != null ? config.get("body").toString() : "";

        logger.info("[github] Creating issue in {}/{}: title='{}'", owner, repo, title);

        try {
            String url = GITHUB_API + "/repos/" + owner + "/" + repo + "/issues";
            Map<String, String> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("body", body);

            String response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "github");
            output.put("action", "create-issue");
            output.put("owner", owner);
            output.put("repo", repo);
            output.put("response", response);
            logger.info("[github] Issue created successfully in {}/{}", owner, repo);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[github] Failed to create issue in {}/{}: {}", owner, repo, e.getMessage());
            return ActionResult.failure("GitHub create issue failed: " + e.getMessage());
        }
    }

    private String getRequired(Map<String, Object> config, String key) {
        Object val = config.get(key);
        return val != null ? val.toString() : null;
    }
}
