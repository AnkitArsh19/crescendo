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

@ActionMapping(appKey = "gitlab", actionKey = "create-issue")
public class GitLabCreateIssueHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GitLabCreateIssueHandler.class);
    private static final String GITLAB_API = "https://gitlab.com/api/v4";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = readCredential(creds, "accessToken", "apiKey");
        if (token == null || token.isBlank()) {
            return ActionResult.failure("GitLab requires 'accessToken' (OAuth2/PAT) in connection credentials");
        }

        String projectId = config.get("projectId") != null ? config.get("projectId").toString() : null;
        String title = config.get("title") != null ? config.get("title").toString() : null;

        if (projectId == null || projectId.isBlank()) return ActionResult.failure("'projectId' is required");
        if (title == null || title.isBlank()) return ActionResult.failure("'title' is required");

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("title", title);
            if (config.containsKey("description")) body.put("description", config.get("description").toString());
            if (config.containsKey("labels")) body.put("labels", config.get("labels").toString());

            String response = RestClient.create()
                    .post()
                    .uri(GITLAB_API + "/projects/" + projectId + "/issues")
                    .header("PRIVATE-TOKEN", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[gitlab] Issue created successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[gitlab] Create issue failed", e);
            return ActionResult.failure("GitLab create issue failed: " + e.getMessage());
        }
    }

    private String readCredential(Map<String, Object> creds, String... keys) {
        if (creds == null) return null;
        for (String key : keys) {
            Object value = creds.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }
}
