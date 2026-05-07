package com.crescendo.apps.linear;

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

@ActionMapping(appKey = "linear", actionKey = "create-issue")
public class LinearCreateIssueHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LinearCreateIssueHandler.class);
    private static final String LINEAR_API = "https://api.linear.app/graphql";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Linear requires an 'accessToken' or 'apiKey' in connection credentials");

        String teamId = config.get("teamId") != null ? config.get("teamId").toString() : null;
        String title = config.get("title") != null ? config.get("title").toString() : null;

        if (teamId == null || teamId.isBlank()) return ActionResult.failure("'teamId' is required");
        if (title == null || title.isBlank()) return ActionResult.failure("'title' is required");

        String description = config.get("description") != null ? config.get("description").toString() : "";
        Object priority = config.get("priority");

        String mutation = """
            mutation {
              issueCreate(input: {
                teamId: "%s"
                title: "%s"
                description: "%s"
                %s
              }) {
                success
                issue { id identifier title url }
              }
            }
            """.formatted(
                teamId,
                title.replace("\"", "\\\""),
                description.replace("\"", "\\\""),
                priority != null ? "priority: " + priority : ""
        );

        try {
            String response = RestClient.create()
                    .post()
                    .uri(LINEAR_API)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("query", mutation))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[linear] Issue created successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[linear] Create issue failed", e);
            return ActionResult.failure("Linear create issue failed: " + e.getMessage());
        }
    }

    private String resolveToken(Map<String, Object> creds) {
        if (creds == null) return null;
      Object token = creds.get("accessToken");
      if (token != null) return token.toString();
      Object key = creds.get("apiKey");
      return key != null ? key.toString() : null;
    }
}
