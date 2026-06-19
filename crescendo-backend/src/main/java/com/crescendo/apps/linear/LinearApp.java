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
        var teamField = Map.of("key", "teamId", "label", "Team",
                "type", "dynamic_dropdown", "resourceType", "teams",
                "required", true,
                "helpText", "Select the Linear team");

        var issueField = Map.<String, Object>of("key", "issueId", "label", "Issue",
                "type", "dynamic_dropdown", "resourceType", "issues",
                "dependsOn", List.of("teamId"),
                "required", true,
                "helpText", "Select the issue");

        return new App("linear", "Linear", "Track issues, manage projects, and automate workflows in Linear",
                "https://www.google.com/s2/favicons?domain=linear.app&sz=128", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "issue-created",
                        "name", "Issue Created",
                        "description", "Triggers when a new issue is created",
                        "configSchema", List.of(teamField)
                    ),
                    Map.of(
                        "triggerKey", "issue-updated",
                        "name", "Issue Updated",
                        "description", "Triggers when an issue is modified",
                        "configSchema", List.of(teamField)
                    ),
                    Map.of(
                        "triggerKey", "issue-completed",
                        "name", "Issue Completed",
                        "description", "Triggers when an issue is marked as done",
                        "configSchema", List.of(teamField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "create-issue",
                        "name", "Create Issue",
                        "description", "Create a new issue in Linear",
                        "configSchema", List.of(
                            teamField,
                            Map.of("key", "title", "label", "Issue Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Implement feature X",
                                   "helpText", "Title of the issue"),
                            Map.of("key", "description", "label", "Description",
                                   "type", "textarea", "required", false,
                                   "helpText", "Issue description (Markdown supported)"),
                            Map.of("key", "priority", "label", "Priority",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "0", "label", "No Priority"),
                                       Map.of("value", "1", "label", "Urgent"),
                                       Map.of("value", "2", "label", "High"),
                                       Map.of("value", "3", "label", "Medium"),
                                       Map.of("value", "4", "label", "Low")
                                   ),
                                   "helpText", "Issue priority level"),
                            Map.of("key", "dueDate", "label", "Due Date",
                                   "type", "text", "required", false,
                                   "placeholder", "2025-12-31",
                                   "helpText", "Due date in YYYY-MM-DD format")
                        )
                    ),
                    Map.of(
                        "actionKey", "update-issue",
                        "name", "Update Issue",
                        "description", "Modify an existing issue",
                        "configSchema", List.of(
                            teamField, issueField,
                            Map.of("key", "title", "label", "Title",
                                   "type", "text", "required", false,
                                   "helpText", "Updated title"),
                            Map.of("key", "status", "label", "Status",
                                   "type", "text", "required", false,
                                   "placeholder", "Done",
                                   "helpText", "New status name"),
                            Map.of("key", "priority", "label", "Priority",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "0", "label", "No Priority"),
                                       Map.of("value", "1", "label", "Urgent"),
                                       Map.of("value", "2", "label", "High"),
                                       Map.of("value", "3", "label", "Medium"),
                                       Map.of("value", "4", "label", "Low")
                                   ),
                                   "helpText", "Updated priority")
                        )
                    ),
                    Map.of(
                        "actionKey", "add-comment",
                        "name", "Add Comment",
                        "description", "Add a comment to a Linear issue",
                        "configSchema", List.of(
                            teamField, issueField,
                            Map.of("key", "comment", "label", "Comment",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Looks good, shipping this sprint.",
                                   "helpText", "Comment body (Markdown supported)")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("developer")
        .helpUrl("https://linear.app/settings/api");
    }
}
