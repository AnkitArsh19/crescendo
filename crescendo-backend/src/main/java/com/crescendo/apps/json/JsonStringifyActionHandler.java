package com.crescendo.apps.json;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

@Component
@ActionMapping(appKey = "json", actionKey = "stringify")
public class JsonStringifyActionHandler implements ActionHandler {

    private final JsonActionHandler handler;

    public JsonStringifyActionHandler(JsonActionHandler handler) {
        this.handler = handler;
    }

    @Override
    public ActionResult execute(ActionContext context) {
        return handler.execute(context);
    }
}
