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
 * Grouped handler for Telegram Message operations.
 */
@Component
public class TelegramMessageHandlers {

    private static final String TELEGRAM_API = TelegramSupport.TELEGRAM_API;

    // ── sendMessage ───────────────────────────────────────────────────────────
    @ActionMapping(appKey = "telegram", actionKey = "sendMessage")
    public ActionResult sendMessage(ActionContext context) {
        String token = TelegramSupport.resolveToken(context);
        if (token == null) return TelegramSupport.missingToken();

        Map<String, Object> config = context.configuration();
        String chatId = TelegramSupport.require(config, "chatId");
        String text = TelegramSupport.require(config, "text");

        if (chatId == null || text == null) {
            return ActionResult.failure("'chatId' and 'text' are required");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            
            addCommonMessageOptions(config, body);

            String response = RestClient.create().post()
                    .uri(TELEGRAM_API + token + "/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Telegram sendMessage failed: " + e.getMessage());
        }
    }

    // ── editMessage ───────────────────────────────────────────────────────────
    @ActionMapping(appKey = "telegram", actionKey = "editMessage")
    public ActionResult editMessage(ActionContext context) {
        String token = TelegramSupport.resolveToken(context);
        if (token == null) return TelegramSupport.missingToken();

        Map<String, Object> config = context.configuration();
        String text = TelegramSupport.require(config, "text");
        String chatId = TelegramSupport.opt(config, "chatId", null);
        String messageId = TelegramSupport.opt(config, "messageId", null);
        String inlineMessageId = TelegramSupport.opt(config, "inlineMessageId", null);

        if (text == null) return ActionResult.failure("'text' is required");
        if (inlineMessageId == null && (chatId == null || messageId == null)) {
            return ActionResult.failure("Either 'inlineMessageId' or ('chatId' and 'messageId') must be provided");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("text", text);
            if (inlineMessageId != null) {
                body.put("inline_message_id", inlineMessageId);
            } else {
                body.put("chat_id", chatId);
                body.put("message_id", messageId);
            }
            
            addCommonMessageOptions(config, body);

            String response = RestClient.create().post()
                    .uri(TELEGRAM_API + token + "/editMessageText")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Telegram editMessage failed: " + e.getMessage());
        }
    }

    // ── deleteMessage ─────────────────────────────────────────────────────────
    @ActionMapping(appKey = "telegram", actionKey = "deleteMessage")
    public ActionResult deleteMessage(ActionContext context) {
        String token = TelegramSupport.resolveToken(context);
        if (token == null) return TelegramSupport.missingToken();

        Map<String, Object> config = context.configuration();
        String chatId = TelegramSupport.require(config, "chatId");
        String messageId = TelegramSupport.require(config, "messageId");

        if (chatId == null || messageId == null) {
            return ActionResult.failure("'chatId' and 'messageId' are required");
        }

        try {
            Map<String, Object> body = Map.of("chat_id", chatId, "message_id", messageId);

            String response = RestClient.create().post()
                    .uri(TELEGRAM_API + token + "/deleteMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Telegram deleteMessage failed: " + e.getMessage());
        }
    }

    // ── pinMessage ────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "telegram", actionKey = "pinMessage")
    public ActionResult pinMessage(ActionContext context) {
        String token = TelegramSupport.resolveToken(context);
        if (token == null) return TelegramSupport.missingToken();

        Map<String, Object> config = context.configuration();
        String chatId = TelegramSupport.require(config, "chatId");
        String messageId = TelegramSupport.require(config, "messageId");

        if (chatId == null || messageId == null) {
            return ActionResult.failure("'chatId' and 'messageId' are required");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("message_id", messageId);
            
            String disableNotification = TelegramSupport.opt(config, "disableNotification", null);
            if (disableNotification != null) body.put("disable_notification", Boolean.parseBoolean(disableNotification));

            String response = RestClient.create().post()
                    .uri(TELEGRAM_API + token + "/pinChatMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Telegram pinMessage failed: " + e.getMessage());
        }
    }

    // ── unpinMessage ──────────────────────────────────────────────────────────
    @ActionMapping(appKey = "telegram", actionKey = "unpinMessage")
    public ActionResult unpinMessage(ActionContext context) {
        String token = TelegramSupport.resolveToken(context);
        if (token == null) return TelegramSupport.missingToken();

        Map<String, Object> config = context.configuration();
        String chatId = TelegramSupport.require(config, "chatId");
        if (chatId == null) return ActionResult.failure("'chatId' is required");

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            
            String messageId = TelegramSupport.opt(config, "messageId", null);
            if (messageId != null) body.put("message_id", messageId);

            String endpoint = messageId != null ? "/unpinChatMessage" : "/unpinAllChatMessages";
            
            String response = RestClient.create().post()
                    .uri(TELEGRAM_API + token + endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Telegram unpinMessage failed: " + e.getMessage());
        }
    }

    // ── forwardMessage ────────────────────────────────────────────────────────
    @ActionMapping(appKey = "telegram", actionKey = "forwardMessage")
    public ActionResult forwardMessage(ActionContext context) {
        String token = TelegramSupport.resolveToken(context);
        if (token == null) return TelegramSupport.missingToken();

        Map<String, Object> config = context.configuration();
        String chatId = TelegramSupport.require(config, "chatId");
        String fromChatId = TelegramSupport.require(config, "fromChatId");
        String messageId = TelegramSupport.require(config, "messageId");

        if (chatId == null || fromChatId == null || messageId == null) {
            return ActionResult.failure("'chatId', 'fromChatId', and 'messageId' are required");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("from_chat_id", fromChatId);
            body.put("message_id", messageId);
            
            String disableNotification = TelegramSupport.opt(config, "disableNotification", null);
            if (disableNotification != null) body.put("disable_notification", Boolean.parseBoolean(disableNotification));

            String response = RestClient.create().post()
                    .uri(TELEGRAM_API + token + "/forwardMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("response", response));
        } catch (Exception e) {
            return ActionResult.failure("Telegram forwardMessage failed: " + e.getMessage());
        }
    }

    private void addCommonMessageOptions(Map<String, Object> config, Map<String, Object> body) {
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
    }
}
