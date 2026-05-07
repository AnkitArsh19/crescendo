package com.crescendo.apps.gitlab;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GitLabApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("gitlab", "GitLab", "Manage issues, merge requests, and pipelines",
                "/icons/gitlab.svg", AuthType.OAUTH2,
                List.of(Map.of(
                    "triggerKey", "push",
                    "name", "Push Event",
                    "description", "Triggers on a push to a GitLab repository",
                    "configSchema", List.of(
                        Map.of("key", "projectId", "label", "Project",
                               "type", "dynamic_dropdown", "resourceType", "projects",
                               "required", true,
                               "helpText", "Select the project to watch for pushes")
                    )
                )),
                List.of(Map.of(
                    "actionKey", "create-issue",
                    "name", "Create Issue",
                    "description", "Create a new issue in a GitLab project",
                    "configSchema", List.of(
                        Map.of("key", "projectId", "label", "Project",
                               "type", "dynamic_dropdown", "resourceType", "projects",
                               "required", true,
                               "helpText", "Select the GitLab project"),
                        Map.of("key", "title", "label", "Issue Title",
                               "type", "text", "required", true,
                               "placeholder", "Fix login bug",
                               "helpText", "Title of the issue"),
                        Map.of("key", "description", "label", "Description",
                               "type", "textarea", "required", false,
                               "helpText", "Issue description (Markdown supported)")
                    )
                ))
        )
        .credentialSchema(List.of())
        .category("developer")
        .helpUrl("https://gitlab.com/-/user_settings/applications");
    }
}
