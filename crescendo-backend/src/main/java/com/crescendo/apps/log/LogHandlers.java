package com.crescendo.apps.log;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Internal debug log handler — prints step data to server console.
 */
@Component
public class LogHandlers {

    private static final Logger logger = LoggerFactory.getLogger(LogHandlers.class);

    @ActionMapping(appKey = "crescendo-log", actionKey = "print")
    public ActionResult print(ActionContext context) {
        Map<String, Object> config = context.configuration();
        logger.info("[crescendo-log] DEBUG STEP: {}", config);
        return ActionResult.success(Map.of("logged", true, "data", config));
    }
}
