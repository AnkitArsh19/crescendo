package com.crescendo.apps.errorhandling;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ActionMapping(appKey = "errorhandling", actionKey = "stop-and-error")
public class StopAndErrorActionHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        
        String errorMessage = "Workflow intentionally stopped by Stop and Error node";
        if (config != null && config.containsKey("errorMessage") && config.get("errorMessage") != null) {
            errorMessage = String.valueOf(config.get("errorMessage"));
        }

        // Return a failure result. The WorkflowExecutionEngine will see this
        // and abort the workflow, marking the overall run as FAILED.
        return ActionResult.failure(errorMessage);
    }
}
