package com.crescendo.apps.html;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ActionMapping(appKey = "html-generate", actionKey = "generate")
public class HtmlGenerateActionHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        
        if (config == null || !config.containsKey("html")) {
            return ActionResult.failure("HTML Generate action requires 'html' in configuration");
        }
        
        // The execution engine has already resolved {{steps.X.Y}} templates in the config map.
        // We just need to return the resolved string.
        String html = String.valueOf(config.get("html"));
        
        return ActionResult.success(Map.of("html", html));
    }
}
