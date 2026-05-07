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
 * Adds a comment to a GitHub issue or pull request.
 *
 * <p>Connection credentials: {@code accessToken}
 * <p>Config: {@code owner}, {@code repo}, {@code issueNumber}, {@code body}
 */
@ActionMapping(appKey = "github", actionKey = "create-comment")
public class GitHubCreateCommentHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GitHubCreateCommentHandler.class);
    private static final String GITHUB_API = "https://api.github.com";

    private final RestClient restClient;

    public GitHubCreateCommentHandler() {
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
            return ActionResult.failure("GitHub requires an 'accessToken' in connection credentials");
        }

        String owner = getRequired(config, "owner");
        String repo = getRequired(config, "repo");
        String issueNumber = getRequired(config, "issueNumber");
        String body = getRequired(config, "body");
        if (owner == null) return ActionResult.failure("'owner' is required");
        if (repo == null) return ActionResult.failure("'repo' is required");
        if (issueNumber == null) return ActionResult.failure("'issueNumber' is required");
        if (body == null) return ActionResult.failure("'body' is required");

        try {
            String url = GITHUB_API + "/repos/" + owner + "/" + repo
                    + "/issues/" + issueNumber + "/comments";

            String response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("body", body))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "github");
            output.put("action", "create-comment");
            output.put("owner", owner);
            output.put("repo", repo);
            output.put("issueNumber", issueNumber);
            output.put("response", response);
            logger.info("[github] Comment created successfully");
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[github] Failed to create comment on {}/{}#{}: {}",
                    owner, repo, issueNumber, e.getMessage());
            return ActionResult.failure("GitHub create comment failed: " + e.getMessage());
        }
    }

    private String getRequired(Map<String, Object> config, String key) {
        Object val = config.get(key);
        return val != null ? val.toString() : null;
    }
}
