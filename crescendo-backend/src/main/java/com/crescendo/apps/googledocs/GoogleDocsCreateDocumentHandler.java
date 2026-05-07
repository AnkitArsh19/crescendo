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

@ActionMapping(appKey = "google-docs", actionKey = "create-document")
public class GoogleDocsCreateDocumentHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDocsCreateDocumentHandler.class);
    private static final String DOCS_API = "https://docs.googleapis.com/v1/documents";

    private final RestClient restClient;

    public GoogleDocsCreateDocumentHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = asString(creds != null ? creds.get("accessToken") : null);
        if (accessToken == null || accessToken.isBlank()) {
            logger.warn("[google-docs] create-document: missing accessToken");
            return ActionResult.failure("Google Docs requires an 'accessToken' in connection credentials");
        }

        String title = asString(config.get("title"));
        if (title == null || title.isBlank()) {
            return ActionResult.failure("'title' is required");
        }

        logger.info("[google-docs] Creating document: title='{}'", title);

        try {
            String response = restClient.post()
                    .uri(DOCS_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("title", title))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-docs");
            output.put("response", response);
            logger.info("[google-docs] Document created successfully: title='{}'", title);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-docs] Failed to create document", e);
            return ActionResult.failure("Google Docs create document failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}