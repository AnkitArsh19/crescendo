package com.crescendo.apps.asana;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Asana Comment/Story handlers.
 */
@Component
public class AsanaCommentHandlers {

    private static final String BASE = "https://app.asana.com/api/1.0";

    private String auth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:taskComment:add")
    public Object addComment(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String text = context.getString("text");

        return RestClient.builder()
                .url(BASE + "/tasks/" + taskId + "/stories")
                .header("Authorization", auth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("data", Map.of("text", text)))
                .execute();
    }

    @ActionMapping(appKey = "asana", actionKey = "asana:taskComment:remove")
    public Object removeComment(ActionContext context) throws Exception {
        String commentId = context.getString("commentId");
        return RestClient.builder()
                .url(BASE + "/stories/" + commentId)
                .header("Authorization", auth(context))
                .delete()
                .execute();
    }
}
