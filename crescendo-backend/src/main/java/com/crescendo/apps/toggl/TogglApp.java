package com.crescendo.apps.toggl;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TogglApp implements AppDefinition {

    @Override
    public App toApp() {
        var workspaceField = Map.of("key", "workspaceId", "label", "Workspace",
                "type", "dynamic_dropdown", "resourceType", "workspaces",
                "required", true,
                "helpText", "Select your Toggl workspace");

        var projectField = Map.<String, Object>of("key", "projectId", "label", "Project",
                "type", "dynamic_dropdown", "resourceType", "projects",
                "dependsOn", List.of("workspaceId"),
                "required", false,
                "helpText", "Select the project (optional)");

        return new App("toggl", "Toggl Track", """
                Toggl Track is a time tracking app that offers online time tracking and reporting services. The Crescendo Toggl app lets you automate your personal or team time tracking.

                **What you can do with Toggl Track in Crescendo:**
                - Start a new time entry automatically when a Jira ticket is moved to "In Progress"
                - Generate a weekly summary of billable hours and send it to your accounting channel in Slack
                - Stop your current timer when your Google Calendar indicates you are in a meeting
                - Create a new Toggl Project whenever a client signs a contract in DocuSign

                **Actions available:**
                - Create Time Entry — start tracking time for a specific task
                - Get Current Time Entry — find out what task is currently running
                - Stop Time Entry — halt a running timer

                **Who should use this:** Freelancers, agency owners, and productivity enthusiasts automating their time logs.

                **Authentication:** API Token (available in your Toggl profile settings).
                """,
                "/icons/toggl.svg", AuthType.APIKEY,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-time-entry",
                        "name", "New Time Entry",
                        "description", "Triggers when a new time entry is created",
                        "configSchema", List.of(workspaceField)
                    ),
                    Map.of(
                        "triggerKey", "timer-stopped",
                        "name", "Timer Stopped",
                        "description", "Triggers when a running timer is stopped",
                        "configSchema", List.of(workspaceField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "createTimeEntry",
                        "name", "Create Time Entry",
                        "description", "Log a completed time entry",
                        "configSchema", List.of(
                            workspaceField, projectField,
                            Map.of("key", "description", "label", "Description",
                                   "type", "text", "required", true,
                                   "placeholder", "Working on feature X",
                                   "helpText", "Description of the time entry"),
                            Map.of("key", "startTime", "label", "Start Time",
                                   "type", "text", "required", false,
                                   "placeholder", "2025-03-15T10:00:00Z",
                                   "helpText", "Start time in ISO 8601 (default: now)"),
                            Map.of("key", "duration", "label", "Duration (seconds)",
                                   "type", "text", "required", false,
                                   "placeholder", "3600",
                                   "helpText", "Duration in seconds (-1 for running timer)"),
                            Map.of("key", "tags", "label", "Tags",
                                   "type", "text", "required", false,
                                   "placeholder", "development, frontend",
                                   "helpText", "Comma-separated tags")
                        )
                    ),
                    Map.of(
                        "actionKey", "startTimer",
                        "name", "Start Timer",
                        "description", "Start a new running timer",
                        "configSchema", List.of(
                            workspaceField, projectField,
                            Map.of("key", "description", "label", "Description",
                                   "type", "text", "required", true,
                                   "placeholder", "Working on feature X",
                                   "helpText", "What are you working on?")
                        )
                    ),
                    Map.of(
                        "actionKey", "stopTimer",
                        "name", "Stop Timer",
                        "description", "Stop the currently running timer",
                        "configSchema", List.of(workspaceField)
                    ),
                    Map.of(
                        "actionKey", "getCurrentTimeEntry",
                        "name", "Get Running Timer",
                        "description", "Get the currently running time entry",
                        "configSchema", List.of()
                    )
                )
        )
        .credentialSchema(List.of(
            Map.of("key", "apiToken", "label", "API Token",
                    "type", "password", "required", true,
                    "placeholder", "abc123def456...",
                    "helpText", "Find your API token in Toggl → Profile Settings → API Token")
        ))
        .category("productivity")
        .helpUrl("https://track.toggl.com/profile");
    }
}
