package com.crescendo.apps.brevo;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BrevoAttributeHandlers {

    private String getBaseUrl() {
        return "https://api.brevo.com/v3";
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:attribute:create")
    public Object createAttribute(ActionContext context) throws Exception {
        String category = context.getString("category", "normal");
        String name = context.getString("name");
        String type = context.getString("type");

        Map<String, Object> body = Map.of(
                "type", type
        );

        return RestClient.builder()
                .url(getBaseUrl() + "/contacts/attributes/" + category + "/" + name)
                .header("api-key", context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:attribute:update")
    public Object updateAttribute(ActionContext context) throws Exception {
        String category = context.getString("category", "normal");
        String name = context.getString("name");
        String type = context.getString("type");

        Map<String, Object> body = Map.of(
                "type", type
        );

        return RestClient.builder()
                .url(getBaseUrl() + "/contacts/attributes/" + category + "/" + name)
                .header("api-key", context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:attribute:delete")
    public Object deleteAttribute(ActionContext context) throws Exception {
        String category = context.getString("category", "normal");
        String name = context.getString("name");

        return RestClient.builder()
                .url(getBaseUrl() + "/contacts/attributes/" + category + "/" + name)
                .header("api-key", context.getCredential("apiKey"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:attribute:getAll")
    public Object getAllAttributes(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(getBaseUrl() + "/contacts/attributes")
                .header("api-key", context.getCredential("apiKey"))
                .get()
                .execute();
    }
}
