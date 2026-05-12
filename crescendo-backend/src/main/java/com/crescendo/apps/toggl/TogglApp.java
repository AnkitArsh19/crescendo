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

        return new App("toggl", "Toggl Track", "Track time entries and manage projects in Toggl",
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
                        "actionKey", "create-time-entry",
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
                        "actionKey", "start-timer",
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
                        "actionKey", "stop-timer",
                        "name", "Stop Timer",
                        "description", "Stop the currently running timer",
                        "configSchema", List.of(workspaceField)
                    ),
                    Map.of(
                        "actionKey", "get-current",
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
