package com.crescendo.apps.schedule;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ScheduleApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("schedule", "Schedule Trigger", "Trigger a workflow at a specific time or interval",
                "/icons/schedule.svg", AuthType.NONE,
                List.of(
                    Map.of("triggerKey", "cron", "name", "Cron Schedule",
                        "description", "Triggers based on a Cron expression",
                        "configSchema", List.of(
                            Map.of("key", "cronExpression", "label", "Cron Expression", "type", "text", "required", true,
                                   "placeholder", "0 0 * * * *", "helpText", "Standard 6-field Spring Cron expression (sec min hour day month weekday)")
                        )),
                    Map.of("triggerKey", "interval", "name", "Interval",
                        "description", "Triggers every X minutes",
                        "configSchema", List.of(
                            Map.of("key", "minutes", "label", "Minutes", "type", "number", "required", true,
                                   "placeholder", "15", "helpText", "Trigger every X minutes (min: 1)")
                        ))
                ),
                List.of()
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
