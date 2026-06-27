package com.crescendo.apps.notion;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Notion Block handlers.
 */
@Component
public class NotionBlockHandlers {

    @ActionMapping(appKey = "notion", actionKey = "notion:block:append")
    public Object appendBlock(ActionContext context) throws Exception {
        String blockId = context.getString("blockId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) context.get("children");

        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/blocks/" + blockId + "/children")
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .header("Content-Type", "application/json")
                .patch(Map.of("children", children))
                .execute();
    }

    @ActionMapping(appKey = "notion", actionKey = "notion:block:getAll")
    public Object getAllBlocks(ActionContext context) throws Exception {
        String blockId = context.getString("blockId");
        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/blocks/" + blockId + "/children")
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .get()
                .execute();
    }
}
