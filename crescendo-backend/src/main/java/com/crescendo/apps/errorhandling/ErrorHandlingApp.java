package com.crescendo.apps.errorhandling;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Error Handling.
 */
@Component
public class ErrorHandlingApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "errorhandling",
                "Error Handling",
                """
                Handle workflow errors and trigger errors.
                
                This integration provides operations for:
                - **Stop and Error**: Throw an error in the workflow
                - **Error Trigger**: Triggers the workflow when another workflow has an error
                """,
                "/icons/error.svg", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "errorhandling:stopAndError",
                                "name", "Stop and Error",
                                "description", "Throw an error in the workflow",
                                "configSchema", List.of(
                                        Map.of("key", "errorType", "label", "Error Type", "type", "text", "default", "errorMessage"),
                                        Map.of("key", "errorMessage", "label", "Error Message", "type", "text"),
                                        Map.of("key", "errorObject", "label", "Error Object", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "errorhandling:errorTrigger",
                                "name", "Error Trigger",
                                "description", "Triggers the workflow when another workflow has an error",
                                "configSchema", List.of()
                        )
                )
        ).credentialSchema(List.of()).category("logic-and-flow");
    }
}
