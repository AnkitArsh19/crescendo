package com.crescendo.apps.brevo;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ActionMapping(appKey = "brevo", actionKey = "create-contact")
public class BrevoCreateContactHandler extends BrevoHandler {

    public BrevoCreateContactHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        try {
            String email = value(context, "email", "");
            if (email.isBlank()) return ActionResult.failure("Brevo email is required");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("email", email);
            body.put("updateEnabled", true);
            Object attributes = json(context.configuration().get("attributes"), Map.of());
            Object listIds = json(context.configuration().get("listIds"), List.of());
            if (attributes instanceof Map<?, ?> map && !map.isEmpty()) body.put("attributes", attributes);
            if (listIds instanceof List<?> list && !list.isEmpty()) body.put("listIds", listIds);
            return post(context, "/contacts", body);
        } catch (Exception e) {
            return ActionResult.failure("Brevo create contact failed: " + e.getMessage());
        }
    }
}
