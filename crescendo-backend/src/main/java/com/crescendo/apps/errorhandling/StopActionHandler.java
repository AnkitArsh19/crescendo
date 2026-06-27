package com.crescendo.apps.errorhandling;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ActionMapping(appKey = "errorhandling", actionKey = "stop")
public class StopActionHandler implements ActionHandler {

    @Override
    public ActionResult execute(ActionContext context) {
        // Return a special flag in outputData to indicate graceful stop
        // The workflow execution engine should check for this flag.
        return ActionResult.success(Map.of("_crescendo_stop_execution", true));
    }
}
