package com.crescendo.apps.homeassistant;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey = "home-assistant", actionKey = "call-service")
public class HomeAssistantCallServiceHandler extends HomeAssistantHandler {

    public HomeAssistantCallServiceHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        try {
            String domain = value(context, "domain");
            String service = value(context, "service");
            if (domain.isBlank() || service.isBlank()) {
                return ActionResult.failure("Home Assistant domain and service are required");
            }
            return post(context, "/api/services/{domain}/{service}", json(context.configuration().get("data")), domain, service);
        } catch (Exception e) {
            return ActionResult.failure("Home Assistant service call failed: " + e.getMessage());
        }
    }
}
