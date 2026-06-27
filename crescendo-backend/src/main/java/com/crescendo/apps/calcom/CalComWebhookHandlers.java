package com.crescendo.apps.calcom;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Cal.com webhook lifecycle handlers.
 *
 * Based on n8n CalTrigger.node.ts webhookMethods lifecycle:
 *   - subscribe   : POST /hooks (v1) or POST /webhooks (v2)
 *   - list        : GET  /hooks (v1) or GET  /webhooks (v2)
 *   - delete      : DELETE /hooks/{id} (v1) or DELETE /webhooks/{id} (v2)
 *   - getEventTypes: GET  /event-types — loadOptions used in the trigger UI
 *
 * Cal.com changed its API paths in v2.0. We handle both versions based on the
 * apiVersion credential field ("1" = before v2, "2" = v2.0+).
 */
@Component
public class CalComWebhookHandlers {

    private String getBaseUrl() {
        return "https://api.cal.com/v1";
    }

    private String getHooksPath(ActionContext context) {
        String version = context.getCredential("apiVersion");
        return "2".equals(version) ? "/webhooks" : "/hooks";
    }

    @ActionMapping(appKey = "calcom", actionKey = "calcom:webhook:subscribe")
    public Object subscribeWebhook(ActionContext context) throws Exception {
        String subscriberUrl = context.getString("subscriberUrl");
        List<String> eventTriggers = context.getList("eventTriggers");
        // e.g. ["BOOKING_CREATED", "BOOKING_CANCELLED", "BOOKING_RESCHEDULED", "MEETING_ENDED"]
        String eventTypeId = context.getString("eventTypeId");

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("subscriberUrl", subscriberUrl);
        body.put("eventTriggers", eventTriggers);
        body.put("active", true);
        if (eventTypeId != null && !eventTypeId.isBlank()) {
            body.put("eventTypeId", Integer.parseInt(eventTypeId));
        }

        return RestClient.builder()
                .url(getBaseUrl() + getHooksPath(context))
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "calcom", actionKey = "calcom:webhook:list")
    public Object listWebhooks(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(getBaseUrl() + getHooksPath(context))
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "calcom", actionKey = "calcom:webhook:delete")
    public Object deleteWebhook(ActionContext context) throws Exception {
        String webhookId = context.getString("webhookId");
        return RestClient.builder()
                .url(getBaseUrl() + getHooksPath(context) + "/" + webhookId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "calcom", actionKey = "calcom:eventType:getAll")
    public Object getEventTypes(ActionContext context) throws Exception {
        // Used to populate event type options in the trigger config
        return RestClient.builder()
                .url(getBaseUrl() + "/event-types")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }
}
