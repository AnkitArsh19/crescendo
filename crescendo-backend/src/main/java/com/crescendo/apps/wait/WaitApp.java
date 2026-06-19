package com.crescendo.apps.wait;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WaitApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("wait", "Wait", "Pause workflow execution for a set amount of time",
                "/icons/wait.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "pause", "name", "Pause Execution",
                        "description", "Pause synchronously for short waits or suspend the run for longer waits",
                        "configSchema", List.of(
                            Map.of("key", "mode", "label", "Mode", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "duration", "label", "Duration"),
                                       Map.of("value", "until", "label", "Until Date/Time")
                                   ), "helpText", "How to calculate the wait"),
                            Map.of("key", "seconds", "label", "Seconds", "type", "number", "required", false,
                                   "placeholder", "300", "helpText", "Duration to wait in seconds. Used when Mode is Duration."),
                            Map.of("key", "resumeAt", "label", "Resume At", "type", "text", "required", false,
                                   "placeholder", "2026-06-19T15:30:00+05:30", "helpText", "Date/time to resume. Used when Mode is Until Date/Time."),
                            Map.of("key", "timezone", "label", "Timezone", "type", "text", "required", false,
                                   "placeholder", "Asia/Kolkata", "helpText", "Used for date-only or local date/time values")
                        ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
