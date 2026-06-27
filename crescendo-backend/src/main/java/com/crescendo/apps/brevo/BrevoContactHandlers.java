package com.crescendo.apps.brevo;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BrevoContactHandlers {

    private String getBaseUrl() {
        return "https://api.brevo.com/v3";
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:contact:create")
    public Object createContact(ActionContext context) throws Exception {
        String email = context.getString("email");
        Map<String, Object> attributes = context.getMap("attributes");

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        if (attributes != null) body.put("attributes", attributes);

        return RestClient.builder()
                .url(getBaseUrl() + "/contacts")
                .header("api-key", context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:contact:update")
    public Object updateContact(ActionContext context) throws Exception {
        String identifier = context.getString("identifier"); // Email or ID
        Map<String, Object> attributes = context.getMap("attributes");

        Map<String, Object> body = new HashMap<>();
        if (attributes != null) body.put("attributes", attributes);

        return RestClient.builder()
                .url(getBaseUrl() + "/contacts/" + java.net.URLEncoder.encode(identifier, "UTF-8"))
                .header("api-key", context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:contact:upsert")
    public Object upsertContact(ActionContext context) throws Exception {
        String email = context.getString("email");
        Map<String, Object> attributes = context.getMap("attributes");

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        if (attributes != null) body.put("attributes", attributes);
        body.put("updateEnabled", true);

        return RestClient.builder()
                .url(getBaseUrl() + "/contacts")
                .header("api-key", context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:contact:get")
    public Object getContact(ActionContext context) throws Exception {
        String identifier = context.getString("identifier");

        return RestClient.builder()
                .url(getBaseUrl() + "/contacts/" + java.net.URLEncoder.encode(identifier, "UTF-8"))
                .header("api-key", context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:contact:getAll")
    public Object getAllContacts(ActionContext context) throws Exception {
        int limit = context.getInt("limit", 50);

        return RestClient.builder()
                .url(getBaseUrl() + "/contacts?limit=" + limit)
                .header("api-key", context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:contact:delete")
    public Object deleteContact(ActionContext context) throws Exception {
        String identifier = context.getString("identifier");

        return RestClient.builder()
                .url(getBaseUrl() + "/contacts/" + java.net.URLEncoder.encode(identifier, "UTF-8"))
                .header("api-key", context.getCredential("apiKey"))
                .delete()
                .execute();
    }
}
