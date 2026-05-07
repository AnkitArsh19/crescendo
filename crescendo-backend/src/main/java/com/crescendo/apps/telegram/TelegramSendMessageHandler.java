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

@ActionMapping(appKey = "telegram", actionKey = "send-message")
public class TelegramSendMessageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TelegramSendMessageHandler.class);
    private static final String TELEGRAM_API = "https://api.telegram.org/bot";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String botToken = creds != null ? (String) creds.get("apiKey") : null;
        if (botToken == null || botToken.isBlank()) {
            return ActionResult.failure("Telegram requires 'apiKey' (bot token) in connection credentials");
        }

        String chatId = config.get("chatId") != null ? config.get("chatId").toString() : null;
        String text = config.get("text") != null ? config.get("text").toString() : null;

        if (chatId == null || chatId.isBlank()) return ActionResult.failure("'chatId' is required");
        if (text == null || text.isBlank()) return ActionResult.failure("'text' is required");

        String parseMode = config.getOrDefault("parseMode", "Markdown").toString();

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", parseMode);

            String response = RestClient.create()
                    .post()
                    .uri(TELEGRAM_API + botToken + "/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[telegram] Message sent successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[telegram] Send message failed", e);
            return ActionResult.failure("Telegram send message failed: " + e.getMessage());
        }
    }
}
