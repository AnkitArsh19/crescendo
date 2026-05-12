package com.crescendo.apps.pomodoro;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class PomodoroApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("pomodoro", "Pomodoro Timer", "Focus timers, work logging, and time calculation",
                "/icons/pomodoro.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "create-timer", "name", "Start Focus Timer",
                        "description", "Start a Pomodoro focus session",
                        "configSchema", List.of(
                            Map.of("key", "durationMinutes", "label", "Duration (minutes)", "type", "text", "required", false,
                                   "placeholder", "25", "helpText", "Focus duration (default: 25 min)"),
                            Map.of("key", "label", "label", "Session Label", "type", "text", "required", false,
                                   "placeholder", "Deep work session", "helpText", "Optional label"))),
                    Map.of("actionKey", "log-work-time", "name", "Log Work Time",
                        "description", "Record a completed work session",
                        "configSchema", List.of(
                            Map.of("key", "task", "label", "Task", "type", "text", "required", false,
                                   "placeholder", "Code review", "helpText", "What you worked on"),
                            Map.of("key", "durationMinutes", "label", "Duration (minutes)", "type", "text", "required", true,
                                   "placeholder", "25", "helpText", "Time spent in minutes"))),
                    Map.of("actionKey", "calculate-end-time", "name", "Calculate End Time",
                        "description", "Calculate when a timer will end",
                        "configSchema", List.of(
                            Map.of("key", "startTime", "label", "Start Time", "type", "text", "required", true,
                                   "placeholder", "2026-05-01T10:00:00Z", "helpText", "ISO 8601 start time"),
                            Map.of("key", "durationMinutes", "label", "Duration (minutes)", "type", "text", "required", true,
                                   "placeholder", "25", "helpText", "Session duration")))
                )
        ).credentialSchema(List.of()).category("productivity").helpUrl("");
    }
}
