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
                                        Map.of("key", "resume", "label", "Resume", "type", "text", "default", "timeInterval"),
                                        Map.of("key", "amount", "label", "Amount", "type", "number", "default", 1),
                                        Map.of("key", "unit", "label", "Unit", "type", "text", "default", "hours"),
                                        Map.of("key", "dateTime", "label", "Date and Time", "type", "text"),
                                        Map.of("key", "incomingAuthentication", "label", "Authentication", "type", "text", "default", "none"),
                                        Map.of("key", "limitWaitTime", "label", "Limit Wait Time", "type", "boolean", "default", false),
                                        Map.of("key", "limitType", "label", "Limit Type", "type", "text", "default", "afterTimeInterval"),
                                        Map.of("key", "resumeAmount", "label", "Resume Amount", "type", "number", "default", 1),
                                        Map.of("key", "resumeUnit", "label", "Resume Unit", "type", "text", "default", "hours"),
                                        Map.of("key", "maxDateAndTime", "label", "Max Date and Time", "type", "text"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("logic-and-flow");
    }
}
