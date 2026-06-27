package com.crescendo.apps.freshdesk;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FreshdeskTicketHandlers {

    private String getBaseUrl(ActionContext context) {
        String domain = context.getCredential("domain");
        if (domain != null && !domain.startsWith("http")) {
            domain = "https://" + domain;
        }
        return domain + "/api/v2";
    }

    private String getAuth(ActionContext context) {
        String apiKey = context.getCredential("apiKey");
        String authStr = apiKey + ":X";
        return "Basic " + java.util.Base64.getEncoder().encodeToString(authStr.getBytes());
    }

    @ActionMapping(appKey = "freshdesk", actionKey = "freshdesk:ticket:create")
    public Object createTicket(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        if (payload == null) payload = new HashMap<>();
        
        String email = context.getString("email");
        String subject = context.getString("subject");
        String description = context.getString("description");
        Integer status = context.getInt("status");
        Integer priority = context.getInt("priority");

        if (email != null) payload.put("email", email);
        if (subject != null) payload.put("subject", subject);
        if (description != null) payload.put("description", description);
        if (status != null) payload.put("status", status);
        if (priority != null) payload.put("priority", priority);

        return RestClient.builder()
                .url(getBaseUrl(context) + "/tickets")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "freshdesk", actionKey = "freshdesk:ticket:update")
    public Object updateTicket(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String ticketId = context.getString("ticketId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/tickets/" + ticketId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(payload)
                .execute();
    }

    @ActionMapping(appKey = "freshdesk", actionKey = "freshdesk:ticket:get")
    public Object getTicket(ActionContext context) throws Exception {
        String ticketId = context.getString("ticketId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/tickets/" + ticketId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "freshdesk", actionKey = "freshdesk:ticket:getAll")
    public Object getAllTickets(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(getBaseUrl(context) + "/tickets")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "freshdesk", actionKey = "freshdesk:ticket:delete")
    public Object deleteTicket(ActionContext context) throws Exception {
        String ticketId = context.getString("ticketId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/tickets/" + ticketId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
