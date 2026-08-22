package com.crescendo.apps.wait;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Wait.
 */
@Component
public class WaitApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "wait",
                "Wait",
                """
                Wait before continue with execution.
                
                This integration provides operations for:
                - **Wait**: Waits for a certain amount of time, until a specific date and time, or for a webhook call/form submission before continuing
                """,
                "/icons/wait.svg", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "wait:wait",
                                "name", "Wait",
                                "description", "Wait before continue with execution",
                                "configSchema", List.of(
                                        Map.of("key", "resume", "label", "Resume Mode", "type", "select", "required", true,
                                                "options", List.of(
                                                        Map.of("value", "timeInterval", "label", "Wait for Duration"),
                                                        Map.of("value", "specificTime", "label", "Wait until Specific Date & Time")
                                                ), "default", "timeInterval", "helpText", "Choose whether to wait for a duration or until a specific timestamp"),
                                        Map.of("key", "amount", "label", "Duration Amount", "type", "number", "default", 1, "helpText", "Number of time units to wait"),
                                        Map.of("key", "unit", "label", "Time Unit", "type", "select", "required", true,
                                                "options", List.of(
                                                        Map.of("value", "seconds", "label", "Seconds"),
                                                        Map.of("value", "minutes", "label", "Minutes"),
                                                        Map.of("value", "hours", "label", "Hours"),
                                                        Map.of("value", "days", "label", "Days"),
                                                        Map.of("value", "weeks", "label", "Weeks")
                                                ), "default", "minutes", "helpText", "Unit of time to wait"),
                                        Map.of("key", "dateTime", "label", "Date and Time", "type", "datetime", "default", "{{now + 1h}}",
                                                "placeholder", "{{now + 1h}} or pick date/time", "helpText", "Exact datetime to resume execution"),
                                        Map.of("key", "limitWaitTime", "label", "Limit Wait Time", "type", "boolean", "default", false),
                                        Map.of("key", "limitType", "label", "Limit Type", "type", "select",
                                                "options", List.of(
                                                        Map.of("value", "afterTimeInterval", "label", "After Time Interval"),
                                                        Map.of("value", "atSpecificTime", "label", "At Specific Date & Time")
                                                ), "default", "afterTimeInterval"),
                                        Map.of("key", "resumeAmount", "label", "Max Wait Amount", "type", "number", "default", 1),
                                        Map.of("key", "resumeUnit", "label", "Max Wait Unit", "type", "select",
                                                "options", List.of(
                                                        Map.of("value", "seconds", "label", "Seconds"),
                                                        Map.of("value", "minutes", "label", "Minutes"),
                                                        Map.of("value", "hours", "label", "Hours"),
                                                        Map.of("value", "days", "label", "Days")
                                                ), "default", "hours"),
                                        Map.of("key", "maxDateAndTime", "label", "Max Date and Time", "type", "datetime", "default", "{{now + 1d}}")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("logic");
    }
}
