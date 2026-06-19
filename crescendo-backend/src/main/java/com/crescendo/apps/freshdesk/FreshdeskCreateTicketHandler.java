package com.crescendo.apps.freshdesk;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@ActionMapping(appKey = "freshdesk", actionKey = "create-ticket")
public class FreshdeskCreateTicketHandler extends FreshdeskHandler {

    public FreshdeskCreateTicketHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String email = value(context, "email", "");
        String subject = value(context, "subject", "");
        String description = value(context, "description", "");
        if (email.isBlank() || subject.isBlank() || description.isBlank()) {
            return ActionResult.failure("Freshdesk email, subject, and description are required");
        }
        return post(context, Map.of(
                "email", email,
                "subject", subject,
                "description", description,
                "priority", intValue(context, "priority", 1),
                "status", intValue(context, "status", 2)
        ));
    }
}
