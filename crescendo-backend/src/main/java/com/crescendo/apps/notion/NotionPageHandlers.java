package com.crescendo.apps.notion;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notion Page handlers.
 */
@Component
public class NotionPageHandlers {

    @ActionMapping(appKey = "notion", actionKey = "notion:page:create")
    public Object createPage(ActionContext context) throws Exception {
        String pageId = context.getString("pageId");
        String title = context.getString("title");

        Map<String, Object> body = new HashMap<>();
        body.put("parent", Map.of("page_id", pageId));
        body.put("properties", Map.of("title", List.of(Map.of("text", Map.of("content", title)))));

        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/pages")
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "notion", actionKey = "notion:page:get")
    public Object getPage(ActionContext context) throws Exception {
        String pageId = context.getString("pageId");
        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/pages/" + pageId)
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .get()
                .execute();
    }

    @ActionMapping(appKey = "notion", actionKey = "notion:page:update")
    public Object updatePage(ActionContext context) throws Exception {
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

    @ActionMapping(appKey = "notion", actionKey = "notion:page:archive")
    public Object archivePage(ActionContext context) throws Exception {
        String pageId = context.getString("pageId");
        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/pages/" + pageId)
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .header("Content-Type", "application/json")
                .patch(Map.of("archived", true))
                .execute();
    }

    @ActionMapping(appKey = "notion", actionKey = "notion:page:search")
    public Object searchPage(ActionContext context) throws Exception {
        String query = context.getString("query");
        Map<String, Object> body = new HashMap<>();
        body.put("filter", Map.of("value", "page", "property", "object"));
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
}
