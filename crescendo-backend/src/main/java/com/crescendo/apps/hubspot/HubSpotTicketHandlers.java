package com.crescendo.apps.hubspot;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HubSpotTicketHandlers {

    @ActionMapping(appKey = "hubspot", actionKey = "ticket-create")
    public Object createTicket(ActionContext context) throws Exception {
        Map<String, Object> properties = context.getMap("properties");
        if (properties == null) {
            properties = new java.util.HashMap<>();
        }
        String pipeline = context.getString("pipeline");
        String stage = context.getString("stage");

        if (pipeline != null && !pipeline.isBlank()) {
            properties.put("hs_pipeline", pipeline);
        }
        if (stage != null && !stage.isBlank()) {
            properties.put("hs_pipeline_stage", stage);
        }

        Map<String, Object> body = Map.of("properties", properties);

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/tickets")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "ticket-delete")
    public Object deleteTicket(ActionContext context) throws Exception {
        String ticketId = context.getString("ticketId");

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/tickets/" + ticketId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "ticket-get")
    public Object getTicket(ActionContext context) throws Exception {
        String ticketId = context.getString("ticketId");

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/tickets/" + ticketId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "ticket-getAll")
    public Object getAllTickets(ActionContext context) throws Exception {
        int limit = context.getInt("limit", 100);

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/tickets?limit=" + limit)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "ticket-update")
    public Object updateTicket(ActionContext context) throws Exception {
        String ticketId = context.getString("ticketId");
        Map<String, Object> properties = context.getMap("properties");
        if (properties == null) {
            properties = Map.of();
        }
        Map<String, Object> body = Map.of("properties", properties);

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/tickets/" + ticketId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .patch(body)
                .execute();
    }
}
