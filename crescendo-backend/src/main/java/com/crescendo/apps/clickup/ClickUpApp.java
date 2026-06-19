package com.crescendo.apps.clickup;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ClickUpApp implements AppDefinition {
    public App toApp() {
        return new App(
                "clickup",
                "ClickUp",
                "Create and list ClickUp tasks",
                "https://www.google.com/s2/favicons?domain=clickup.com&sz=128",
                AuthType.OAUTH2,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "create-task",
                                "name", "Create Task",
                                "description", "Create a ClickUp task",
                                "configSchema", List.of(
                                        Map.of("key", "listId", "label", "List ID", "type", "text", "required", true),
                                        Map.of("key", "name", "label", "Name", "type", "text", "required", true),
                                        Map.of("key", "description", "label", "Description", "type", "textarea", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "list-tasks",
                                "name", "List Tasks",
                                "description", "List tasks in a list",
                                "configSchema", List.of(
                                        Map.of("key", "listId", "label", "List ID", "type", "text", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of()).category("productivity").helpUrl("https://clickup.com/api/");
    }
}
