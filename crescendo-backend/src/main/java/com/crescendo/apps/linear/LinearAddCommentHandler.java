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

/**
 * Adds a comment to a Linear issue via GraphQL mutation.
 */
@ActionMapping(appKey = "linear", actionKey = "add-comment")
public class LinearAddCommentHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LinearAddCommentHandler.class);
    private static final String LINEAR_API = "https://api.linear.app/graphql";
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Linear requires an OAuth2 accessToken");
        }

        String issueId = str(config, "issueId");
        String comment = str(config, "comment");
        if (issueId == null) return ActionResult.failure("'issueId' is required");
        if (comment == null) return ActionResult.failure("'comment' is required");

        logger.info("[linear] Adding comment to issue '{}'", issueId);

        try {
            String mutation = "mutation { commentCreate(input: { issueId: \"" + issueId
                    + "\", body: \"" + comment.replace("\"", "\\\"").replace("\n", "\\n") + "\" }) "
                    + "{ success comment { id body createdAt } } }";

            Map<String, Object> response = restClient.post()
                    .uri(LINEAR_API)
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("query", mutation))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "linear");
            output.put("action", "add-comment");
            output.put("issueId", issueId);

            if (response != null && response.containsKey("data")) {
                var data = (Map<String, Object>) response.get("data");
                var commentCreate = (Map<String, Object>) data.get("commentCreate");
                output.put("success", commentCreate.get("success"));
                if (commentCreate.containsKey("comment")) {
                    var commentData = (Map<String, Object>) commentCreate.get("comment");
                    output.put("commentId", commentData.get("id"));
                }
            }
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[linear] Add comment failed: {}", e.getMessage());
            return ActionResult.failure("Linear add-comment failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
