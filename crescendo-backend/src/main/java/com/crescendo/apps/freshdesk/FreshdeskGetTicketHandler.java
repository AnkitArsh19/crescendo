package com.crescendo.apps.freshdesk;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey = "freshdesk", actionKey = "get-ticket")
public class FreshdeskGetTicketHandler extends FreshdeskHandler {

    public FreshdeskGetTicketHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String ticketId = value(context, "ticketId", "");
        if (ticketId.isBlank()) return ActionResult.failure("Freshdesk ticketId is required");
        return get(context, "/api/v2/tickets/{ticketId}", ticketId);
    }
}
