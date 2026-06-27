package com.crescendo.apps.errorhandling;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Error Handling handlers.
 */
@Component
public class ErrorHandlingHandlers {

    @ActionMapping(appKey = "errorhandling", actionKey = "errorhandling:stopAndError")
    public Object stopAndError(ActionContext context) throws Exception {
        String errorType = context.getString("errorType");
        
        if ("errorObject".equals(errorType)) {
            Map<String, Object> errorObject = context.getMap("errorObject");
            throw new RuntimeException("Workflow stopped with error object: " + errorObject);
        } else {
            String errorMessage = context.getString("errorMessage");
            throw new RuntimeException("Workflow stopped with error: " + errorMessage);
        }
    }

    @ActionMapping(appKey = "errorhandling", actionKey = "errorhandling:errorTrigger")
    public Object errorTrigger(ActionContext context) throws Exception {
        // Triggers the workflow when another workflow has an error
        
        return Map.of(
            "status", "success",
            "message", "Error Trigger executed"
        );
    }
}
