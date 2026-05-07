package com.crescendo.apps.whatsapp;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "whatsapp", actionKey = "send-message")
public class WhatsAppSendMessageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppSendMessageHandler.class);
    private static final String GRAPH_API = "https://graph.facebook.com/v19.0";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = resolveToken(creds);
        if (token == null || token.isBlank()) {
            return ActionResult.failure("WhatsApp Business requires an 'accessToken' or 'apiKey' in connection credentials");
        }

        String phoneNumberId = config.get("phoneNumberId") != null ? config.get("phoneNumberId").toString() : null;
        String to = config.get("to") != null ? config.get("to").toString() : null;
        String text = config.get("text") != null ? config.get("text").toString() : null;

        if (phoneNumberId == null || phoneNumberId.isBlank()) return ActionResult.failure("'phoneNumberId' is required");
        if (to == null || to.isBlank()) return ActionResult.failure("'to' is required");
        if (text == null || text.isBlank()) return ActionResult.failure("'text' is required");

        try {
            Map<String, Object> body = Map.of(
                    "messaging_product", "whatsapp",
                    "to", to,
                    "type", "text",
                    "text", Map.of("body", text)
            );

            String response = RestClient.create()
                    .post()
                    .uri(GRAPH_API + "/" + phoneNumberId + "/messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[whatsapp] Message sent successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[whatsapp] Send message failed", e);
            return ActionResult.failure("WhatsApp Business send message failed: " + e.getMessage());
        }
    }

    private String resolveToken(Map<String, Object> creds) {
        if (creds == null) return null;
        Object accessToken = creds.get("accessToken");
        if (accessToken != null) return accessToken.toString();
        Object apiKey = creds.get("apiKey");
        return apiKey != null ? apiKey.toString() : null;
    }
}
