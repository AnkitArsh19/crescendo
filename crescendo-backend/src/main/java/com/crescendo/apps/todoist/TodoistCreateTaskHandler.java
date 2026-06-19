package com.crescendo.apps.todoist;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@ActionMapping(appKey = "todoist", actionKey = "create-task")
public class TodoistCreateTaskHandler extends TodoistHandler {

    public TodoistCreateTaskHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String content = value(context, "content", "");
        if (content.isBlank()) return ActionResult.failure("Todoist content is required");
        Map<String, Object> body = body();
        body.put("content", content);
        putIfPresent(body, "description", value(context, "description", ""));
        putIfPresent(body, "project_id", value(context, "projectId", ""));
        putIfPresent(body, "due_string", value(context, "dueString", ""));
        return post(context, body);
    }

    private void putIfPresent(Map<String, Object> body, String key, String value) {
        if (value != null && !value.isBlank()) body.put(key, value);
    }
}
