package com.crescendo.apps.todoist;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey = "todoist", actionKey = "list-tasks")
public class TodoistListTasksHandler extends TodoistHandler {

    public TodoistListTasksHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String projectId = value(context, "projectId", "");
        if (projectId.isBlank()) return get(context, "/tasks");
        return get(context, "/tasks?project_id={projectId}", projectId);
    }
}
