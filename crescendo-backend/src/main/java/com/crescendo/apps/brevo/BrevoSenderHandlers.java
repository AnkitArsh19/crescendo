package com.crescendo.apps.brevo;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BrevoSenderHandlers {

    private String getBaseUrl() {
        return "https://api.brevo.com/v3";
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:sender:create")
    public Object createSender(ActionContext context) throws Exception {
        String name = context.getString("name");
        String email = context.getString("email");

        Map<String, Object> body = Map.of(
                "name", name,
                "email", email
        );

        return RestClient.builder()
                .url(getBaseUrl() + "/senders")
                .header("api-key", context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:sender:delete")
    public Object deleteSender(ActionContext context) throws Exception {
        String senderId = context.getString("senderId");

        return RestClient.builder()
                .url(getBaseUrl() + "/senders/" + senderId)
                .header("api-key", context.getCredential("apiKey"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:sender:getAll")
    public Object getAllSenders(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(getBaseUrl() + "/senders")
                .header("api-key", context.getCredential("apiKey"))
                .get()
                .execute();
    }
}
