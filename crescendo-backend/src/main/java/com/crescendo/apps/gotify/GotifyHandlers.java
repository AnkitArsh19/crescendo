package com.crescendo.apps.gotify;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import com.crescendo.execution.action.ActionResult;
import org.springframework.web.client.RestClient;
import java.util.Map;
import java.util.HashMap;

@Component
public class GotifyHandlers {

    @ActionMapping(appKey = "gotify", actionKey = "send-message")
    public Object createMessage(ActionContext context) throws Exception {
        String baseUrl = (String) context.credentials().get("baseUrl");
        String appToken = (String) context.credentials().get("appToken");
        
        if (baseUrl == null || baseUrl.isBlank()) {
            return ActionResult.failure("Gotify server URL is missing");
        }
        if (appToken == null || appToken.isBlank()) {
            return ActionResult.failure("Gotify app token is missing");
        }
        
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("message", context.configuration().get("message"));
        
        if (context.configuration().containsKey("title")) {
            body.put("title", context.configuration().get("title"));
        }
        
        if (context.configuration().containsKey("priority")) {
            try {
                body.put("priority", Integer.parseInt(String.valueOf(context.configuration().get("priority"))));
            } catch (NumberFormatException e) {
                // Ignore and use default
            }
        }

        try {
            String response = RestClient.create(baseUrl)
                    .post()
                    .uri("/message")
                    .header("X-Gotify-Key", appToken)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to send Gotify message: " + e.getMessage());
        }
    }
}
