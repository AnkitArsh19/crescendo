package com.crescendo.apps.jira;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class JiraApp implements AppDefinition {
    public App toApp() {
        return new App(
                "jira",
                "Jira",
                "Create and fetch Jira issues",
                "/icons/jira.svg",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "create-issue",
                                "name", "Create Issue",
                                "description", "Create a Jira issue",
                                "configSchema", List.of(
                                        Map.of("key", "projectKey", "label", "Project Key", "type", "text", "required", true),
                                        Map.of("key", "summary", "label", "Summary", "type", "text", "required", true),
                                        Map.of("key", "description", "label", "Description", "type", "textarea", "required", false),
                                        Map.of("key", "issueType", "label", "Issue Type", "type", "text", "required", false, "placeholder", "Task")
                                )
                        ),
                        Map.of(
                                "actionKey", "get-issue",
                                "name", "Get Issue",
                                "description", "Fetch issue by key",
                                "configSchema", List.of(
                                        Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "baseUrl", "label", "Site URL", "type", "text", "required", true),
                Map.of("key", "email", "label", "Email", "type", "text", "required", true),
                Map.of("key", "apiToken", "label", "API Token", "type", "password", "required", true)
        )).category("productivity").helpUrl("https://developer.atlassian.com/cloud/jira/platform/rest/v3/");
    }
}
