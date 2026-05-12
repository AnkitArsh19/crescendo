package com.crescendo.apps.gitlab;

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
 * Creates a merge request in GitLab via POST /api/v4/projects/{id}/merge_requests.
 */
@ActionMapping(appKey = "gitlab", actionKey = "create-mr")
public class GitLabCreateMrHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GitLabCreateMrHandler.class);
    private static final String GITLAB_API = "https://gitlab.com/api/v4";
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("GitLab requires an access token");

        String projectId = str(config, "projectId");
        String title = str(config, "title");
        String sourceBranch = str(config, "sourceBranch");
        String targetBranch = str(config, "targetBranch");
        if (projectId == null) return ActionResult.failure("'projectId' is required");
        if (title == null) return ActionResult.failure("'title' is required");
        if (sourceBranch == null) return ActionResult.failure("'sourceBranch' is required");
        if (targetBranch == null) return ActionResult.failure("'targetBranch' is required");

        String description = str(config, "description");

        logger.info("[gitlab] Creating MR '{}' in project '{}'", title, projectId);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("title", title);
            body.put("source_branch", sourceBranch);
            body.put("target_branch", targetBranch);
            if (description != null) body.put("description", description);

            String url = GITLAB_API + "/projects/" + java.net.URLEncoder.encode(projectId, "UTF-8") + "/merge_requests";
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .header("PRIVATE-TOKEN", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "gitlab");
            output.put("action", "create-mr");
            output.put("mrIid", response != null ? response.get("iid") : null);
            output.put("webUrl", response != null ? response.get("web_url") : null);
            output.put("title", title);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[gitlab] Create MR failed: {}", e.getMessage());
            return ActionResult.failure("GitLab create-mr failed: " + e.getMessage());
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
