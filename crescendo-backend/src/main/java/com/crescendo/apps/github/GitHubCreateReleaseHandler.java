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
 * Creates a release on a GitHub repository via POST /repos/{owner}/{repo}/releases.
 */
@ActionMapping(appKey = "github", actionKey = "create-release")
public class GitHubCreateReleaseHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GitHubCreateReleaseHandler.class);
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
        String tagName = str(config, "tagName");
        if (repo == null) return ActionResult.failure("'repo' is required");
        if (tagName == null) return ActionResult.failure("'tagName' is required");

        String name = str(config, "name");
        String body = str(config, "body");
        boolean draft = "true".equalsIgnoreCase(str(config, "draft"));
        boolean prerelease = "true".equalsIgnoreCase(str(config, "prerelease"));

        logger.info("[github] Creating release '{}' on repo '{}'", tagName, repo);

        try {
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("tag_name", tagName);
            if (name != null) reqBody.put("name", name);
            if (body != null) reqBody.put("body", body);
            reqBody.put("draft", draft);
            reqBody.put("prerelease", prerelease);

            String url = GITHUB_API + "/repos/" + repo + "/releases";
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reqBody)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "github");
            output.put("action", "create-release");
            output.put("releaseId", response != null ? response.get("id") : null);
            output.put("htmlUrl", response != null ? response.get("html_url") : null);
            output.put("tagName", tagName);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[github] Create release failed: {}", e.getMessage());
            return ActionResult.failure("GitHub create-release failed: " + e.getMessage());
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
