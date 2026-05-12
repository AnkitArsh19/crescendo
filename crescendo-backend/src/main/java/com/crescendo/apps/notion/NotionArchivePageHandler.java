package com.crescendo.apps.notion;

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
 * Archives a Notion page via PATCH /v1/pages/{pageId} with archived=true.
 */
@ActionMapping(appKey = "notion", actionKey = "archive-page")
public class NotionArchivePageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotionArchivePageHandler.class);
    private static final String NOTION_API = "https://api.notion.com/v1/pages/";
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null || token.isBlank()) {
            return ActionResult.failure("Notion requires an OAuth2 accessToken");
        }

        String pageId = str(config, "pageId");
        if (pageId == null) return ActionResult.failure("'pageId' is required");

        logger.info("[notion] Archiving page '{}'", pageId);

        try {
            Map<String, Object> response = restClient.patch()
                    .uri(NOTION_API + pageId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("Notion-Version", "2022-06-28")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("archived", true))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "notion");
            output.put("action", "archive-page");
            output.put("pageId", pageId);
            output.put("archived", true);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[notion] Archive page failed: {}", e.getMessage());
            return ActionResult.failure("Notion archive-page failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
