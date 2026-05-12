package com.crescendo.apps.googledocs;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Retrieves the content and metadata of a Google Doc via Docs API v1.
 */
@ActionMapping(appKey = "google-docs", actionKey = "get-document")
public class GoogleDocsGetDocumentHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDocsGetDocumentHandler.class);
    private static final String DOCS_API = "https://docs.googleapis.com/v1/documents/";

    private final RestClient restClient;

    public GoogleDocsGetDocumentHandler() {
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
        if (documentId == null) return ActionResult.failure("'documentId' is required");

        logger.info("[google-docs] Getting document '{}'", documentId);

        try {
            Map<String, Object> doc = restClient.get()
                    .uri(DOCS_API + documentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-docs");
            output.put("action", "get-document");
            output.put("documentId", documentId);
            output.put("title", doc != null ? doc.get("title") : null);
            output.put("revisionId", doc != null ? doc.get("revisionId") : null);

            // Extract text content from body
            if (doc != null && doc.containsKey("body")) {
                var body = (Map<String, Object>) doc.get("body");
                var content = body.get("content");
                StringBuilder textContent = new StringBuilder();
                if (content instanceof java.util.List<?> elements) {
                    for (Object el : elements) {
                        if (el instanceof Map<?,?> elem) {
                            var paragraph = (Map<String, Object>) ((Map<?,?>) elem).get("paragraph");
                            if (paragraph != null) {
                                var paragElements = (java.util.List<?>) paragraph.get("elements");
                                if (paragElements != null) {
                                    for (Object pe : paragElements) {
                                        if (pe instanceof Map<?,?> peMap) {
                                            var textRun = (Map<String, Object>) ((Map<?,?>) peMap).get("textRun");
                                            if (textRun != null) {
                                                textContent.append(textRun.get("content"));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                output.put("textContent", textContent.toString());
            }

            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-docs] Get document failed: {}", e.getMessage());
            return ActionResult.failure("Google Docs get-document failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
