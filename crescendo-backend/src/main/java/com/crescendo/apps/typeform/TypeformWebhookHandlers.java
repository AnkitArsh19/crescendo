package com.crescendo.apps.typeform;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Typeform Webhook Handlers.
 *
 * Implements the webhook lifecycle from n8n TypeformTrigger.node.ts:
 *   - create : PUT /forms/{formId}/webhooks/{webhookId}
 *   - delete : DELETE /forms/{formId}/webhooks/{webhookId}
 */
@Component
public class TypeformWebhookHandlers {

    private String getBaseUrl() {
        return "https://api.typeform.com";
    }

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "typeform", actionKey = "typeform:webhook:subscribe")
    public Object subscribeWebhook(ActionContext context) throws Exception {
        String formId = context.getString("formId");
        String webhookUrl = context.getString("webhookUrl");

        // n8n generates a unique webhookId per registration
        String webhookId = "crescendo-" + UUID.randomUUID().toString().substring(0, 10);

        Map<String, Object> body = new HashMap<>();
        body.put("url", webhookUrl);
        body.put("enabled", true);
        body.put("verify_ssl", true);
        
        // Optional: generate and send a secret if we want to verify signatures later
        // String webhookSecret = java.util.UUID.randomUUID().toString().replace("-", "");
        // body.put("secret", webhookSecret);

        Object response = RestClient.builder()
                .url(getBaseUrl() + "/forms/" + formId + "/webhooks/" + webhookId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(body)
                .execute();

        // Return the webhookId so the Crescendo framework can store it
        Map<String, Object> result = new HashMap<>();
        result.put("webhookId", webhookId);
        result.put("response", response);
        return result;
    }

    @ActionMapping(appKey = "typeform", actionKey = "typeform:webhook:delete")
    public Object deleteWebhook(ActionContext context) throws Exception {
        String formId = context.getString("formId");
        String webhookId = context.getString("webhookId");

        return RestClient.builder()
                .url(getBaseUrl() + "/forms/" + formId + "/webhooks/" + webhookId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
