package com.crescendo.apps.todoist;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Todoist Comment handlers.
 */
@Component
public class TodoistCommentHandlers {

    private String getBaseUrl() {
        return "https://api.todoist.com/rest/v2";
    }

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("apiKey");
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:comment:create")
    public Object createComment(ActionContext context) throws Exception {
        String content = context.getString("content");
        String taskId = context.getString("taskId");
        String projectId = context.getString("projectId");

        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        if (taskId != null) body.put("task_id", taskId);
        if (projectId != null) body.put("project_id", projectId);

        return RestClient.builder()
                .url(getBaseUrl() + "/comments")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:comment:get")
    public Object getComment(ActionContext context) throws Exception {
        String commentId = context.getString("commentId");
        return RestClient.builder()
                .url(getBaseUrl() + "/comments/" + commentId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:comment:getAll")
    public Object getAllComments(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        String projectId = context.getString("projectId");

        String url = getBaseUrl() + "/comments?";
        if (taskId != null) url += "task_id=" + taskId + "&";
        if (projectId != null) url += "project_id=" + projectId;

        return RestClient.builder()
                .url(url)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:comment:update")
    public Object updateComment(ActionContext context) throws Exception {
        String commentId = context.getString("commentId");
        String content = context.getString("content");

        return RestClient.builder()
                .url(getBaseUrl() + "/comments/" + commentId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("content", content))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:comment:delete")
    public Object deleteComment(ActionContext context) throws Exception {
        String commentId = context.getString("commentId");
        return RestClient.builder()
                .url(getBaseUrl() + "/comments/" + commentId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
