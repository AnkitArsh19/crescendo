package com.crescendo.apps.telegram;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Telegram bot resources using the Bot API.
 * Lists recent chats where the bot has received messages.
 * Supports: chats
 */
@Component
public class TelegramResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(TelegramResourceProvider.class);
    private static final String TELEGRAM_API = "https://api.telegram.org";

    @Override
    public String appKey() {
        return "telegram";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("chats");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String botToken = credentials.get("botToken").toString();
        return listChats(botToken);
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listChats(String botToken) {
        try {
            // getUpdates returns recent messages — we extract unique chats
            Map<String, Object> response = RestClient.builder()
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .get()
                    .uri(TELEGRAM_API + "/bot{token}/getUpdates?limit=100", botToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> result = (List<Map<String, Object>>) response.get("result");
            if (result == null) return List.of();

            // Deduplicate by chat ID
            Map<String, ResourceOption> chatMap = new LinkedHashMap<>();
            for (Map<String, Object> update : result) {
                Map<String, Object> message = (Map<String, Object>) update.get("message");
                if (message == null) continue;

                Map<String, Object> chat = (Map<String, Object>) message.get("chat");
                if (chat == null) continue;

                String chatId = String.valueOf(((Number) chat.get("id")).longValue());
                if (chatMap.containsKey(chatId)) continue;

                String type = chat.get("type") != null ? chat.get("type").toString() : "private";
                String label;
                if ("private".equals(type)) {
                    String firstName = chat.get("first_name") != null ? chat.get("first_name").toString() : "";
                    String lastName = chat.get("last_name") != null ? " " + chat.get("last_name") : "";
                    label = (firstName + lastName).trim();
                } else {
                    label = chat.get("title") != null ? chat.get("title").toString() : chatId;
                }

                chatMap.put(chatId, new ResourceOption(chatId, label, type));
            }

            return new ArrayList<>(chatMap.values());
        } catch (Exception e) {
            logger.error("[telegram] Failed to list chats: {}", e.getMessage());
            return List.of();
        }
    }
}
