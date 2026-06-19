package com.crescendo.apps.todoist;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TodoistApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("todoist", "Todoist", "Create and list Todoist tasks",
                "https://www.google.com/s2/favicons?domain=todoist.com&sz=128", AuthType.OAUTH2,
                List.of(),
                List.of(
                        Map.of("actionKey", "create-task", "name", "Create Task",
                                "description", "Create a Todoist task",
                                "configSchema", List.of(
                                        Map.of("key", "content", "label", "Content", "type", "text", "required", true),
                                        Map.of("key", "description", "label", "Description", "type", "textarea", "required", false),
                                        Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", false),
                                        Map.of("key", "dueString", "label", "Due", "type", "text", "required", false,
                                                "placeholder", "tomorrow at 9am"))),
                        Map.of("actionKey", "list-tasks", "name", "List Tasks",
                                "description", "List active Todoist tasks",
                                "configSchema", List.of(
                                        Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", false)))
                )
        ).credentialSchema(List.of()).category("productivity").helpUrl("https://developer.todoist.com/rest/v2/");
    }
}
