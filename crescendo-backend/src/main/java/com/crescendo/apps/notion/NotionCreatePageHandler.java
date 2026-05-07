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

@ActionMapping(appKey = "notion", actionKey = "create-page")
public class NotionCreatePageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotionCreatePageHandler.class);
    private static final String NOTION_API = "https://api.notion.com/v1";
    private static final String NOTION_VERSION = "2022-06-28";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Notion requires an 'accessToken' or 'apiKey' in connection credentials");

        String databaseId = config.get("databaseId") != null ? config.get("databaseId").toString() : null;
        if (databaseId == null || databaseId.isBlank()) {
            return ActionResult.failure("'databaseId' is required");
        }

        Object properties = config.get("properties");
        if (properties == null) {
            return ActionResult.failure("'properties' is required");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("parent", Map.of("database_id", databaseId));
            body.put("properties", properties);

            String response = RestClient.create()
                    .post()
                    .uri(NOTION_API + "/pages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("Notion-Version", NOTION_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[notion] Page created successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[notion] Create page failed", e);
            return ActionResult.failure("Notion create page failed: " + e.getMessage());
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
