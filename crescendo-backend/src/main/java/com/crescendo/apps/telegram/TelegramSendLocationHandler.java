package com.crescendo.apps.telegram;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends a location to a Telegram chat via sendLocation API.
 */
@ActionMapping(appKey = "telegram", actionKey = "send-location")
public class TelegramSendLocationHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TelegramSendLocationHandler.class);
    private static final String TELEGRAM_API = "https://api.telegram.org/bot";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String botToken = creds != null ? (String) creds.get("apiKey") : null;
        if (botToken == null) botToken = creds != null ? (String) creds.get("botToken") : null;
        if (botToken == null || botToken.isBlank()) {
            return ActionResult.failure("Telegram requires 'apiKey' (bot token)");
        }

        String chatId = str(config, "chatId");
        String latitude = str(config, "latitude");
        String longitude = str(config, "longitude");
        if (chatId == null) return ActionResult.failure("'chatId' is required");
        if (latitude == null) return ActionResult.failure("'latitude' is required");
        if (longitude == null) return ActionResult.failure("'longitude' is required");

        logger.info("[telegram] Sending location to chat '{}'", chatId);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("latitude", Double.parseDouble(latitude));
            body.put("longitude", Double.parseDouble(longitude));

            String response = RestClient.create()
                    .post()
                    .uri(TELEGRAM_API + botToken + "/sendLocation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "telegram");
            output.put("action", "send-location");
            output.put("chatId", chatId);
            output.put("latitude", latitude);
            output.put("longitude", longitude);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[telegram] Send location failed: {}", e.getMessage());
            return ActionResult.failure("Telegram send-location failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
