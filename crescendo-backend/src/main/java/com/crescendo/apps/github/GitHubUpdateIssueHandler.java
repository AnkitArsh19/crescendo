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
 * Updates an existing GitHub issue via PATCH /repos/{owner}/{repo}/issues/{number}.
 */
@ActionMapping(appKey = "github", actionKey = "update-issue")
public class GitHubUpdateIssueHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GitHubUpdateIssueHandler.class);
    private static final String GITHUB_API = "https://api.github.com";
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("GitHub requires a token");

        String repo = str(config, "repo");
        String issueNumber = str(config, "issueNumber");
        if (repo == null) return ActionResult.failure("'repo' is required");
        if (issueNumber == null) return ActionResult.failure("'issueNumber' is required");

        logger.info("[github] Updating issue #{} on repo '{}'", issueNumber, repo);

        try {
            Map<String, Object> patch = new HashMap<>();
            if (config.containsKey("title") && config.get("title") != null)
                patch.put("title", config.get("title"));
            if (config.containsKey("body") && config.get("body") != null)
                patch.put("body", config.get("body"));
            if (config.containsKey("state") && config.get("state") != null)
                patch.put("state", config.get("state"));
            if (config.containsKey("labels") && config.get("labels") != null) {
                String labelsStr = config.get("labels").toString();
                patch.put("labels", java.util.Arrays.asList(labelsStr.split(",")));
            }

            String url = GITHUB_API + "/repos/" + repo + "/issues/" + issueNumber;
            Map<String, Object> response = restClient.patch()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "github");
            output.put("action", "update-issue");
            output.put("issueNumber", issueNumber);
            output.put("title", response != null ? response.get("title") : null);
            output.put("state", response != null ? response.get("state") : null);
            output.put("htmlUrl", response != null ? response.get("html_url") : null);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[github] Update issue failed: {}", e.getMessage());
            return ActionResult.failure("GitHub update-issue failed: " + e.getMessage());
        }
    }

    private String resolveToken(Map<String, Object> creds) {
        if (creds == null) return null;
        String t = (String) creds.get("accessToken");
        if (t == null) t = (String) creds.get("apiKey");
        return (t != null && !t.isBlank()) ? t : null;
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
