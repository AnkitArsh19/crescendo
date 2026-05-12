package com.crescendo.apps.gitlab;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Closes an issue in GitLab via PUT /api/v4/projects/{id}/issues/{iid} with state_event=close.
 */
@ActionMapping(appKey = "gitlab", actionKey = "close-issue")
public class GitLabCloseIssueHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GitLabCloseIssueHandler.class);
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
        String issueIid = str(config, "issueIid");
        if (projectId == null) return ActionResult.failure("'projectId' is required");
        if (issueIid == null) return ActionResult.failure("'issueIid' is required");

        logger.info("[gitlab] Closing issue #{} in project '{}'", issueIid, projectId);

        try {
            String url = GITLAB_API + "/projects/"
                    + java.net.URLEncoder.encode(projectId, "UTF-8")
                    + "/issues/" + issueIid;

            Map<String, Object> response = restClient.put()
                    .uri(url)
                    .header("PRIVATE-TOKEN", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("state_event", "close"))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "gitlab");
            output.put("action", "close-issue");
            output.put("issueIid", issueIid);
            output.put("state", response != null ? response.get("state") : "closed");
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[gitlab] Close issue failed: {}", e.getMessage());
            return ActionResult.failure("GitLab close-issue failed: " + e.getMessage());
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
