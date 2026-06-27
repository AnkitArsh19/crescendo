package com.crescendo.apps.notion;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Notion Database handlers.
 */
@Component
public class NotionDatabaseHandlers {

    // ─── DATABASE ───

    @ActionMapping(appKey = "notion", actionKey = "notion:database:get")
    public Object getDatabase(ActionContext context) throws Exception {
        String databaseId = context.getString("databaseId");
        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/databases/" + databaseId)
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .get()
                .execute();
    }

    @ActionMapping(appKey = "notion", actionKey = "notion:database:getAll")
    public Object getAllDatabases(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/search")
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .header("Content-Type", "application/json")
                .post(Map.of("filter", Map.of("value", "database", "property", "object")))
                .execute();
    }

    @ActionMapping(appKey = "notion", actionKey = "notion:database:search")
    public Object searchDatabase(ActionContext context) throws Exception {
        String query = context.getString("query");
        Map<String, Object> body = new HashMap<>();
        body.put("filter", Map.of("value", "database", "property", "object"));
        if (query != null && !query.isBlank()) {
            body.put("query", query);
        }

        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/search")
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    // ─── DATABASE PAGE ───

    @ActionMapping(appKey = "notion", actionKey = "notion:databasePage:create")
    public Object createDatabasePage(ActionContext context) throws Exception {
        String databaseId = context.getString("databaseId");
        Map<String, Object> properties = context.getMap("properties");

        Map<String, Object> body = new HashMap<>();
        body.put("parent", Map.of("database_id", databaseId));
        body.put("properties", properties);

        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/pages")
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "notion", actionKey = "notion:databasePage:get")
    public Object getDatabasePage(ActionContext context) throws Exception {
        String pageId = context.getString("pageId");
        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/pages/" + pageId)
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .get()
                .execute();
    }

    @ActionMapping(appKey = "notion", actionKey = "notion:databasePage:getAll")
    public Object getAllDatabasePages(ActionContext context) throws Exception {
        String databaseId = context.getString("databaseId");
        Map<String, Object> filter = context.getMap("filter");

        Map<String, Object> body = new HashMap<>();
        if (filter != null && !filter.isEmpty()) {
            body.put("filter", filter);
        }

        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/databases/" + databaseId + "/query")
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "notion", actionKey = "notion:databasePage:update")
    public Object updateDatabasePage(ActionContext context) throws Exception {
        String pageId = context.getString("pageId");
        Map<String, Object> properties = context.getMap("properties");

        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/pages/" + pageId)
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .header("Content-Type", "application/json")
                .patch(Map.of("properties", properties))
                .execute();
    }
}
