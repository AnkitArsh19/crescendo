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
        return new App("pomodoro", "Pomodoro Timer", "Schedule focus timers and calculate work sessions",
                "/icons/pomodoro.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of(
                        "actionKey", "create-timer",
                        "name", "Create Timer",
                        "description", "Start a Pomodoro focus timer — outputs startTime, endTime, and duration for downstream steps",
                        "configSchema", List.of(
                            Map.of("key", "durationMinutes", "label", "Focus Duration (minutes)",
                                   "type", "number", "required", false,
                                   "placeholder", "25",
                                   "helpText", "Duration of the focus session in minutes (default: 25)"),
                            Map.of("key", "label", "label", "Timer Label",
                                   "type", "text", "required", false,
                                   "placeholder", "Deep work session",
                                   "helpText", "Optional label for this timer session")
                        )
                    ),
                    Map.of(
                        "actionKey", "calculate-end-time",
                        "name", "Calculate End Time",
                        "description", "Calculate when a timer will end based on start time and duration",
                        "configSchema", List.of(
                            Map.of("key", "startTime", "label", "Start Time",
                                   "type", "text", "required", true,
                                   "placeholder", "2026-05-01T10:00:00Z",
                                   "helpText", "Start time in ISO 8601 format"),
                            Map.of("key", "durationMinutes", "label", "Duration (minutes)",
                                   "type", "number", "required", true,
                                   "placeholder", "25",
                                   "helpText", "Duration of the session in minutes")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("");
    }
}
