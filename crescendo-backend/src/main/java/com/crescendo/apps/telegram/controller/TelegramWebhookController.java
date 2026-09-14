package com.crescendo.apps.telegram.controller;

import com.crescendo.apps.telegram.service.TelegramLinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public webhook endpoint for Telegram Bot API updates.
 * Configured via setWebhook to: https://api.crescendo.run/webhooks/telegram/bot
 */
@RestController
@RequestMapping("/webhooks/telegram")
public class TelegramWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final TelegramLinkService linkService;

    public TelegramWebhookController(TelegramLinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping("/bot")
    public ResponseEntity<Map<String, Object>> handleTelegramUpdate(@RequestBody(required = false) Map<String, Object> update) {
        if (update == null || update.isEmpty()) {
            return ResponseEntity.ok(Map.of("ok", true));
        }

        try {
            // 1. Check for private direct message with /start <token>
            if (update.get("message") instanceof Map m) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) m;
                @SuppressWarnings("unchecked")
                Map<String, Object> from = message.get("from") instanceof Map f ? (Map<String, Object>) f : null;
                @SuppressWarnings("unchecked")
                Map<String, Object> chat = message.get("chat") instanceof Map c ? (Map<String, Object>) c : null;

                Long tgUserId = extractLong(from, "id");
                String username = extractString(from, "username");
                String firstName = extractString(from, "first_name");
                Long chatId = extractLong(chat, "id");
                String text = message.get("text") != null ? message.get("text").toString().trim() : "";
                String chatType = extractString(chat, "type");

                if (text.startsWith("/start ") && text.length() > 7) {
                    String token = text.substring(7).trim();

                    if (token.length() >= 16 && tgUserId != null && chatId != null) {
                        logger.info("[telegram-webhook] Received /start with token {} from user {} (@{})", token, tgUserId, username);
                        boolean linked = linkService.handleStartCommand(token, tgUserId, username, firstName, chatId);
                        if (linked) {
                            logger.info("[telegram-webhook] Successfully linked token {} for user {}", token, tgUserId);
                        }
                    }
                } else if ("private".equalsIgnoreCase(chatType) && (text.equalsIgnoreCase("/start") || text.equalsIgnoreCase("/help") || text.equalsIgnoreCase("hi") || text.equalsIgnoreCase("hello"))) {
                    logger.info("[telegram-webhook] Received plain '{}' in direct chat from user {} (@{})", text, tgUserId, username);
                    linkService.handlePlainStartCommand(tgUserId, chatId, firstName);
                } else {
                    // Standard message in group/channel
                    linkService.handleMessageUpdate(message);
                }
            }

            // 2. Check for bot added/removed from channel or group (my_chat_member)
            if (update.get("my_chat_member") instanceof Map mcm) {
                @SuppressWarnings("unchecked")
                Map<String, Object> myChatMember = (Map<String, Object>) mcm;
                logger.info("[telegram-webhook] Received my_chat_member event: {}", myChatMember);
                linkService.handleChatMemberUpdate(myChatMember);
            }

            // 3. Check for channel post
            if (update.get("channel_post") instanceof Map cp) {
                @SuppressWarnings("unchecked")
                Map<String, Object> post = (Map<String, Object>) cp;
                linkService.handleMessageUpdate(post);
            }

        } catch (Exception e) {
            logger.error("[telegram-webhook] Error processing Telegram update: {}", e.getMessage(), e);
        }

        // Always return HTTP 200 OK so Telegram does not retry
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private String extractString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString().trim() : null;
    }

    private Long extractLong(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s.trim()); } catch (Exception ignored) {}
        }
        return null;
    }
}
