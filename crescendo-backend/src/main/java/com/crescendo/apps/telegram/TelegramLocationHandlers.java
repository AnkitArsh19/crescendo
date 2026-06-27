package com.crescendo.apps.telegram;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Telegram Location operations.
 */
@Component
public class TelegramLocationHandlers {

    private static final String TELEGRAM_API = TelegramSupport.TELEGRAM_API;

    // ── sendLocation ──────────────────────────────────────────────────────────
    @ActionMapping(appKey = "telegram", actionKey = "sendLocation")
    public ActionResult sendLocation(ActionContext context) {
        String token = TelegramSupport.resolveToken(context);
        if (token == null) return TelegramSupport.missingToken();

        Map<String, Object> config = context.configuration();
        String chatId = TelegramSupport.require(config, "chatId");
        String latitude = TelegramSupport.require(config, "latitude");
        String longitude = TelegramSupport.require(config, "longitude");

        if (chatId == null || latitude == null || longitude == null) {
            return ActionResult.failure("'chatId', 'latitude', and 'longitude' are required");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("latitude", Float.parseFloat(latitude));
            body.put("longitude", Float.parseFloat(longitude));

            String disableNotification = TelegramSupport.opt(config, "disableNotification", null);
            if (disableNotification != null) body.put("disable_notification", Boolean.parseBoolean(disableNotification));

            String replyToMessageId = TelegramSupport.opt(config, "replyToMessageId", null);
            if (replyToMessageId != null) body.put("reply_to_message_id", replyToMessageId);

            String replyMarkup = TelegramSupport.opt(config, "replyMarkup", null);
            if (replyMarkup != null) {
                try {
                    body.put("reply_markup", TelegramSupport.getMapper().readValue(replyMarkup, Object.class));
                } catch (Exception ignored) {
                }
            }

            String response = RestClient.create().post()
                    .uri(TELEGRAM_API + token + "/sendLocation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Telegram sendLocation failed: " + e.getMessage());
        }
    }
}
