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
import java.util.Map;

@ActionMapping(appKey = "google-docs", actionKey = "append-text")
public class GoogleDocsAppendTextHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDocsAppendTextHandler.class);
    private static final String DOCS_API = "https://docs.googleapis.com/v1/documents";

    private final RestClient restClient;

    public GoogleDocsAppendTextHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = asString(creds != null ? creds.get("accessToken") : null);
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Docs requires an 'accessToken' in connection credentials");
        }

        String documentId = asString(config.get("documentId"));
        String text = asString(config.get("text"));
        if (documentId == null || documentId.isBlank()) return ActionResult.failure("'documentId' is required");
        if (text == null || text.isBlank()) return ActionResult.failure("'text' is required");

        Map<String, Object> body = Map.of(
                "requests", java.util.List.of(Map.of(
                        "insertText", Map.of(
                                "endOfSegmentLocation", Map.of(),
                                "text", text
                        )
                ))
        );

        try {
            String response = restClient.post()
                    .uri(DOCS_API + "/" + documentId + ":batchUpdate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-docs");
            output.put("documentId", documentId);
            output.put("response", response);
            logger.info("[google-docs] Text appended successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-docs] Failed to append text", e);
            return ActionResult.failure("Google Docs append text failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}