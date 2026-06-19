package com.crescendo.apps.homeassistant;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey = "home-assistant", actionKey = "get-state")
public class HomeAssistantGetStateHandler extends HomeAssistantHandler {

    public HomeAssistantGetStateHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String entityId = value(context, "entityId");
        if (entityId.isBlank()) return ActionResult.failure("Home Assistant entityId is required");
        return get(context, "/api/states/{entityId}", entityId);
    }
}
