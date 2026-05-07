package com.crescendo.apps.log;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Debug/logging action — logs input data and passes it through.
 */
@ActionMapping(appKey = "crescendo-log", actionKey = "print")
public class LogPrintHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LogPrintHandler.class);

    @Override
    public ActionResult execute(ActionContext context) {
        logger.info("[log-action] app={}, action={}, input={}",
                context.appKey(), context.actionKey(), context.inputData());
        return ActionResult.success(context.inputData() != null ? context.inputData() : Map.of());
    }
}
