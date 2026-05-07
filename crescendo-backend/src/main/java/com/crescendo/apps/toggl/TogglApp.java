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
        return new App("toggl", "Toggl Track", "Track time entries and manage projects",
                "/icons/toggl.svg", AuthType.APIKEY,
                List.of(Map.of(
                    "triggerKey", "timer-stopped",
                    "name", "Timer Stopped",
                    "description", "Triggers when a time entry is completed"
                )),
                List.of(Map.of(
                    "actionKey", "create-time-entry",
                    "name", "Create Time Entry",
                    "description", "Start a new time entry in Toggl",
                    "configSchema", Map.of(
                        "description", "string (required) — time entry description",
                        "projectId", "integer — Toggl project ID",
                        "duration", "integer — duration in seconds (-1 for running)"
                    )
                ))
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
