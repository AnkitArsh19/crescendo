package com.crescendo.apps.clickup;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ClickUp Comment handlers.
 */
@Component
public class ClickUpCommentHandlers {

    private String getBaseUrl() {
        return "https://api.clickup.com/api/v2";
    }

    private String getAuth(ActionContext context) {
        return context.getCredential("apiToken");
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:comment:create")
    public Object createComment(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String commentText = context.getString("commentText");

        return RestClient.builder()
                .url(getBaseUrl() + "/task/" + taskId + "/comment")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("comment_text", commentText))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:comment:delete")
    public Object deleteComment(ActionContext context) throws Exception {
        String commentId = context.getString("commentId");
        return RestClient.builder()
                .url(getBaseUrl() + "/comment/" + commentId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:comment:get")
    public Object getComment(ActionContext context) throws Exception {
        String commentId = context.getString("commentId");
        return RestClient.builder()
                .url(getBaseUrl() + "/comment/" + commentId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:comment:getAll")
    public Object getAllComments(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        return RestClient.builder()
                .url(getBaseUrl() + "/task/" + taskId + "/comment")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:comment:update")
    public Object updateComment(ActionContext context) throws Exception {
        String commentId = context.getString("commentId");
        String commentText = context.getString("commentText");

        return RestClient.builder()
                .url(getBaseUrl() + "/comment/" + commentId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(Map.of("comment_text", commentText))
                .execute();
    }
}
