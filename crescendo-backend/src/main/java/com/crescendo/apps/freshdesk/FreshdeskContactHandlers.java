package com.crescendo.apps.freshdesk;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FreshdeskContactHandlers {

    private String getBaseUrl(ActionContext context) {
        String domain = context.getCredential("domain");
        if (domain != null && !domain.startsWith("http")) {
            domain = "https://" + domain;
        }
        return domain + "/api/v2";
    }

    private String getAuth(ActionContext context) {
        String apiKey = context.getCredential("apiKey");
        // Freshdesk uses API Key as username and "X" as password, encoded in base64
        String authStr = apiKey + ":X";
        return "Basic " + java.util.Base64.getEncoder().encodeToString(authStr.getBytes());
    }

    @ActionMapping(appKey = "freshdesk", actionKey = "freshdesk:contact:create")
    public Object createContact(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        if (payload == null) payload = new HashMap<>();
        
        String name = context.getString("name");
        String email = context.getString("email");
        if (name != null) payload.put("name", name);
        if (email != null) payload.put("email", email);

        return RestClient.builder()
                .url(getBaseUrl(context) + "/contacts")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }

    @ActionMapping(appKey = "freshdesk", actionKey = "freshdesk:contact:update")
    public Object updateContact(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String contactId = context.getString("contactId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/contacts/" + contactId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(payload)
                .execute();
    }

    @ActionMapping(appKey = "freshdesk", actionKey = "freshdesk:contact:get")
    public Object getContact(ActionContext context) throws Exception {
        String contactId = context.getString("contactId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/contacts/" + contactId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "freshdesk", actionKey = "freshdesk:contact:getAll")
    public Object getAllContacts(ActionContext context) throws Exception {
        // Fetch up to limit
        return RestClient.builder()
                .url(getBaseUrl(context) + "/contacts")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "freshdesk", actionKey = "freshdesk:contact:delete")
    public Object deleteContact(ActionContext context) throws Exception {
        String contactId = context.getString("contactId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/contacts/" + contactId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
