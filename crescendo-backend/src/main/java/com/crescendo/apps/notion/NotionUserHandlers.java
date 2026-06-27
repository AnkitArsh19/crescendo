package com.crescendo.apps.notion;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

/**
 * Notion User handlers.
 */
@Component
public class NotionUserHandlers {

    @ActionMapping(appKey = "notion", actionKey = "notion:user:get")
    public Object getUser(ActionContext context) throws Exception {
        String userId = context.getString("userId");
        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/users/" + userId)
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .get()
                .execute();
    }

    @ActionMapping(appKey = "notion", actionKey = "notion:user:getAll")
    public Object getAllUsers(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(NotionSupport.getBaseUrl() + "/users")
                .header("Authorization", NotionSupport.getAuthHeader(context))
                .header("Notion-Version", NotionSupport.getVersionHeader())
                .get()
                .execute();
    }
}
