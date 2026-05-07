package com.crescendo.apps.linear;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LinearApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("linear", "Linear", "Track issues and manage projects",
                "/icons/linear.svg", AuthType.OAUTH2,
                List.of(Map.of(
                    "triggerKey", "issue-created",
                    "name", "Issue Created",
                    "description", "Triggers when a new issue is created",
                    "configSchema", List.of(
                        Map.of("key", "teamId", "label", "Team",
                               "type", "dynamic_dropdown", "resourceType", "teams",
                               "required", true,
                               "helpText", "Select the team to watch for new issues")
                    )
                )),
                List.of(Map.of(
                    "actionKey", "create-issue",
                    "name", "Create Issue",
                    "description", "Create a new issue in Linear",
                    "configSchema", List.of(
                        Map.of("key", "teamId", "label", "Team",
                               "type", "dynamic_dropdown", "resourceType", "teams",
                               "required", true,
                               "helpText", "Select the team to create the issue in"),
                        Map.of("key", "title", "label", "Issue Title",
                               "type", "text", "required", true,
                               "placeholder", "Implement feature X",
                               "helpText", "Title of the issue"),
                        Map.of("key", "description", "label", "Description",
                               "type", "textarea", "required", false,
                               "helpText", "Issue description (Markdown supported)")
                    )
                ))
        )
        .credentialSchema(List.of())
        .category("developer")
        .helpUrl("https://linear.app/settings/api");
    }
}
