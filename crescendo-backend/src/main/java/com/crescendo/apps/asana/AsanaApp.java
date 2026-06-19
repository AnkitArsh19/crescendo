package com.crescendo.apps.asana;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;

import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AsanaApp implements AppDefinition {
    public App toApp() {
        return new App("asana", "Asana", "Create and list Asana tasks", "https://www.google.com/s2/favicons?domain=asana.com&sz=128", AuthType.OAUTH2, List.of(),
                List.of(Map.of("actionKey", "create-task", "name", "Create Task", "description", "Create an Asana task",
                        "configSchema",
                        List.of(Map.of("key", "workspace", "label", "Workspace GID", "type", "text", "required", true),
                                Map.of("key", "name", "label", "Name", "type", "text", "required", true),
                                Map.of("key", "notes", "label", "Notes", "type", "textarea", "required", false),
                                Map.of("key", "projects", "label", "Project GIDs CSV", "type", "text", "required",
                                        false))),
                        Map.of("actionKey", "list-tasks", "name", "List Tasks", "description",
                                "List tasks in a project", "configSchema",
                                List.of(Map.of("key", "project", "label", "Project GID", "type", "text", "required",
                                        true)))))
                .credentialSchema(List.of()).category("productivity").helpUrl("https://developers.asana.com/docs/");
    }
}
