package com.crescendo.apps.errorhandling;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ErrorHandlingApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("error-handling", "Error Handling", "Trigger on errors or deliberately stop a workflow",
                "/icons/error.svg", AuthType.NONE,
                List.of(
                    Map.of("triggerKey", "error-trigger", "name", "Error Trigger",
                        "description", "Triggers when another workflow fails",
                        "configSchema", List.of(
                            Map.of("key", "workflowId", "label", "Workflow ID (Optional)", "type", "text", "required", false,
                                   "placeholder", "Leave blank for ANY workflow", "helpText", "ID of the workflow to monitor")
                        ))
                ),
                List.of(
                    Map.of("actionKey", "stop-and-error", "name", "Stop and Error",
                        "description", "Stops the workflow and throws a custom error",
                        "configSchema", List.of(
                            Map.of("key", "errorMessage", "label", "Error Message", "type", "text", "required", true,
                                   "placeholder", "Validation failed: User not found", "helpText", "The message that will appear in the execution logs")
                        ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
