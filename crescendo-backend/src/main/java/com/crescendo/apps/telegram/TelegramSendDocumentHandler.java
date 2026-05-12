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
 * Sends a document to a Telegram chat via sendDocument API.
 */
@ActionMapping(appKey = "telegram", actionKey = "send-document")
public class TelegramSendDocumentHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TelegramSendDocumentHandler.class);
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
        String document = str(config, "document");
        if (chatId == null) return ActionResult.failure("'chatId' is required");
        if (document == null) return ActionResult.failure("'document' URL is required");

        String caption = str(config, "caption");

        logger.info("[telegram] Sending document to chat '{}'", chatId);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("document", document);
            if (caption != null) body.put("caption", caption);

            String response = RestClient.create()
                    .post()
                    .uri(TELEGRAM_API + botToken + "/sendDocument")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "telegram");
            output.put("action", "send-document");
            output.put("chatId", chatId);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[telegram] Send document failed: {}", e.getMessage());
            return ActionResult.failure("Telegram send-document failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
