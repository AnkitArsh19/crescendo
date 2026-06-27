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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActionMapping(appKey = "linear", actionKey = "update-issue")
public class LinearUpdateIssueHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LinearUpdateIssueHandler.class);
    private static final String LINEAR_API = "https://api.linear.app/graphql";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = resolveToken(creds);
        if (token == null || token.isBlank()) {
            return ActionResult.failure("Linear requires an 'accessToken' or 'apiKey' in connection credentials");
        }

        String issueId = config.get("issueId") != null ? config.get("issueId").toString() : null;
        if (issueId == null || issueId.isBlank()) return ActionResult.failure("'issueId' is required");

        List<String> fields = new ArrayList<>();
        if (config.containsKey("title")) {
            fields.add("title: \"" + config.get("title").toString().replace("\"", "\\\"") + "\"");
        }
        if (config.containsKey("stateId")) {
            fields.add("stateId: \"" + config.get("stateId") + "\"");
        }
        if (config.containsKey("priority")) {
            fields.add("priority: " + config.get("priority"));
        }

        if (fields.isEmpty()) {
            return ActionResult.failure("At least one field to update is required (title, stateId, priority)");
        }

        String mutation = """
            mutation {
              issueUpdate(id: "%s", input: { %s }) {
                success
                issue { id identifier title url }
              }
            }
            """.formatted(issueId, String.join(", ", fields));

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
            logger.info("[linear] Issue updated successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[linear] Update issue failed", e);
            return ActionResult.failure("Linear update issue failed: " + e.getMessage());
        }
    }

    private String resolveToken(Map<String, Object> creds) {
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        if (token != null) return "Bearer " + token.toString();
        Object apiKey = creds.get("apiKey");
        return apiKey != null ? apiKey.toString() : null;
    }
}
