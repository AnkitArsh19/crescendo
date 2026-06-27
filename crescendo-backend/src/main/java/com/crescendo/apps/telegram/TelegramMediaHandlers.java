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
 * Grouped handler for Telegram Media operations.
 */
@Component
public class TelegramMediaHandlers {

    private static final String TELEGRAM_API = TelegramSupport.TELEGRAM_API;

    @ActionMapping(appKey = "telegram", actionKey = "sendPhoto")
    public ActionResult sendPhoto(ActionContext context) {
        return sendMedia(context, "sendPhoto", "photo");
    }

    @ActionMapping(appKey = "telegram", actionKey = "sendVideo")
    public ActionResult sendVideo(ActionContext context) {
        return sendMedia(context, "sendVideo", "video");
    }

    @ActionMapping(appKey = "telegram", actionKey = "sendAudio")
    public ActionResult sendAudio(ActionContext context) {
        return sendMedia(context, "sendAudio", "audio");
    }

    @ActionMapping(appKey = "telegram", actionKey = "sendDocument")
    public ActionResult sendDocument(ActionContext context) {
        return sendMedia(context, "sendDocument", "document");
    }

    @ActionMapping(appKey = "telegram", actionKey = "sendAnimation")
    public ActionResult sendAnimation(ActionContext context) {
        return sendMedia(context, "sendAnimation", "animation");
    }

    @ActionMapping(appKey = "telegram", actionKey = "sendVoice")
    public ActionResult sendVoice(ActionContext context) {
        return sendMedia(context, "sendVoice", "voice");
    }

    @ActionMapping(appKey = "telegram", actionKey = "sendSticker")
    public ActionResult sendSticker(ActionContext context) {
        return sendMedia(context, "sendSticker", "sticker");
    }

    private ActionResult sendMedia(ActionContext context, String endpoint, String mediaField) {
        String token = TelegramSupport.resolveToken(context);
        if (token == null) return TelegramSupport.missingToken();

        Map<String, Object> config = context.configuration();
        String chatId = TelegramSupport.require(config, "chatId");
        String fileIdOrUrl = TelegramSupport.require(config, mediaField);

        if (chatId == null || fileIdOrUrl == null) {
            return ActionResult.failure("'chatId' and '" + mediaField + "' are required");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put(mediaField, fileIdOrUrl);
            
            String caption = TelegramSupport.opt(config, "caption", null);
            if (caption != null) body.put("caption", caption);

            String parseMode = TelegramSupport.opt(config, "parseMode", "Markdown");
            if (parseMode != null && !parseMode.isBlank()) body.put("parse_mode", parseMode);

            String replyToMessageId = TelegramSupport.opt(config, "replyToMessageId", null);
            if (replyToMessageId != null) body.put("reply_to_message_id", replyToMessageId);

            String disableNotification = TelegramSupport.opt(config, "disableNotification", null);
            if (disableNotification != null) body.put("disable_notification", Boolean.parseBoolean(disableNotification));

            String replyMarkup = TelegramSupport.opt(config, "replyMarkup", null);
            if (replyMarkup != null) {
                try {
                    body.put("reply_markup", TelegramSupport.getMapper().readValue(replyMarkup, Object.class));
                } catch (Exception ignored) {
                }
            }

            String response = RestClient.create().post()
                    .uri(TELEGRAM_API + token + "/" + endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Telegram " + endpoint + " failed: " + e.getMessage());
        }
    }

    // ── sendContact ───────────────────────────────────────────────────────────
    @ActionMapping(appKey = "telegram", actionKey = "sendContact")
    public ActionResult sendContact(ActionContext context) {
        String token = TelegramSupport.resolveToken(context);
        if (token == null) return TelegramSupport.missingToken();

        Map<String, Object> config = context.configuration();
        String chatId = TelegramSupport.require(config, "chatId");
        String phoneNumber = TelegramSupport.require(config, "phoneNumber");
        String firstName = TelegramSupport.require(config, "firstName");

        if (chatId == null || phoneNumber == null || firstName == null) {
            return ActionResult.failure("'chatId', 'phoneNumber', and 'firstName' are required");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("phone_number", phoneNumber);
            body.put("first_name", firstName);
            
            String lastName = TelegramSupport.opt(config, "lastName", null);
            if (lastName != null) body.put("last_name", lastName);

            String response = RestClient.create().post()
                    .uri(TELEGRAM_API + token + "/sendContact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Telegram sendContact failed: " + e.getMessage());
        }
    }

    // ── sendVenue ─────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "telegram", actionKey = "sendVenue")
    public ActionResult sendVenue(ActionContext context) {
        String token = TelegramSupport.resolveToken(context);
        if (token == null) return TelegramSupport.missingToken();

        Map<String, Object> config = context.configuration();
        String chatId = TelegramSupport.require(config, "chatId");
        String latitude = TelegramSupport.require(config, "latitude");
        String longitude = TelegramSupport.require(config, "longitude");
        String title = TelegramSupport.require(config, "title");
        String address = TelegramSupport.require(config, "address");

        if (chatId == null || latitude == null || longitude == null || title == null || address == null) {
            return ActionResult.failure("'chatId', 'latitude', 'longitude', 'title', and 'address' are required");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("latitude", Float.parseFloat(latitude));
            body.put("longitude", Float.parseFloat(longitude));
            body.put("title", title);
            body.put("address", address);

            String response = RestClient.create().post()
                    .uri(TELEGRAM_API + token + "/sendVenue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Telegram sendVenue failed: " + e.getMessage());
        }
    }
}
