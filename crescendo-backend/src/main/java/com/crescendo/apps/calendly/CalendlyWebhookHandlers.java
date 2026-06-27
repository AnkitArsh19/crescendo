package com.crescendo.apps.calendly;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Calendly webhook subscription management handlers.
 *
 * Based on n8n CalendlyTriggerV2.node.ts webhook lifecycle:
 *   - subscribeWebhook   : POST /webhook_subscriptions — registers a webhook for invitee.created or invitee.canceled
 *   - listWebhooks       : GET  /webhook_subscriptions — lists existing subscriptions for scope
 *   - deleteWebhook      : DELETE /{webhookUri} — removes a webhook subscription by URI
 *
 * These are called by the Crescendo trigger lifecycle to manage webhook registrations.
 */
@Component
public class CalendlyWebhookHandlers {

    private static final String BASE_URL = "https://api.calendly.com";

    @ActionMapping(appKey = "calendly", actionKey = "calendly:webhook:subscribe")
    public Object subscribeWebhook(ActionContext context) throws Exception {
        String callbackUrl = context.getString("callbackUrl");
        List<String> events = context.getList("events");     // e.g. ["invitee.created", "invitee.canceled"]
        String scope = context.getString("scope", "user");    // "user" or "organization"
        String organization = context.getString("organization");
        String userUri = context.getString("userUri");

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("url", callbackUrl);
        body.put("events", events);
        body.put("organization", organization);
        body.put("scope", scope);
        if ("user".equals(scope) && userUri != null) {
            body.put("user", userUri);
        }

        return RestClient.builder()
                .url(BASE_URL + "/webhook_subscriptions")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "calendly", actionKey = "calendly:webhook:list")
    public Object listWebhooks(ActionContext context) throws Exception {
        String organization = context.getString("organization");
        String scope = context.getString("scope", "user");
        String userUri = context.getString("userUri");

        StringBuilder url = new StringBuilder(BASE_URL + "/webhook_subscriptions?organization=" + organization + "&scope=" + scope);
        if ("user".equals(scope) && userUri != null) {
            url.append("&user=").append(userUri);
        }

        return RestClient.builder()
                .url(url.toString())
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "calendly", actionKey = "calendly:webhook:delete")
    public Object deleteWebhook(ActionContext context) throws Exception {
        // Calendly uses the full webhook URI (e.g. https://api.calendly.com/webhook_subscriptions/{uuid})
        String webhookUri = context.getString("webhookUri");

        return RestClient.builder()
                .url(webhookUri)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "calendly", actionKey = "calendly:user:getMe")
    public Object getCurrentUser(ActionContext context) throws Exception {
        // Used internally to get organization URI and user URI for webhook scoping
        return RestClient.builder()
                .url(BASE_URL + "/users/me")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }
}
