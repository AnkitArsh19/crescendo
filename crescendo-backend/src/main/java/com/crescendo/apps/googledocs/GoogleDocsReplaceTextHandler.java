package com.crescendo.apps.googledocs;

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
import java.util.List;
import java.util.Map;

/**
 * Replaces text in a Google Doc using batchUpdate with replaceAllText.
 */
@ActionMapping(appKey = "google-docs", actionKey = "replace-text")
public class GoogleDocsReplaceTextHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDocsReplaceTextHandler.class);
    private static final String DOCS_API = "https://docs.googleapis.com/v1/documents/";

    private final RestClient restClient;

    public GoogleDocsReplaceTextHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Docs requires an OAuth2 accessToken");
        }

        String documentId = str(config, "documentId");
        String searchText = str(config, "searchText");
        String replaceText = str(config, "replaceText");
        if (documentId == null) return ActionResult.failure("'documentId' is required");
        if (searchText == null) return ActionResult.failure("'searchText' is required");
        if (replaceText == null) return ActionResult.failure("'replaceText' is required");

        boolean matchCase = "true".equalsIgnoreCase(str(config, "matchCase"));

        logger.info("[google-docs] Replacing '{}' with '{}' in document '{}'", searchText, replaceText, documentId);

        try {
            Map<String, Object> request = Map.of(
                "requests", List.of(
                    Map.of("replaceAllText", Map.of(
                        "containsText", Map.of(
                            "text", searchText,
                            "matchCase", matchCase
                        ),
                        "replaceText", replaceText
                    ))
                )
            );

            Map<String, Object> response = restClient.post()
                    .uri(DOCS_API + documentId + ":batchUpdate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            int occurrences = 0;
            if (response != null && response.containsKey("replies")) {
                var replies = (List<Map<String, Object>>) response.get("replies");
                if (!replies.isEmpty()) {
                    var replaceResult = (Map<String, Object>) replies.get(0).get("replaceAllText");
                    if (replaceResult != null && replaceResult.containsKey("occurrencesChanged")) {
                        occurrences = ((Number) replaceResult.get("occurrencesChanged")).intValue();
                    }
                }
            }

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-docs");
            output.put("action", "replace-text");
            output.put("documentId", documentId);
            output.put("searchText", searchText);
            output.put("replaceText", replaceText);
            output.put("occurrencesChanged", occurrences);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-docs] Replace text failed: {}", e.getMessage());
            return ActionResult.failure("Google Docs replace-text failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
