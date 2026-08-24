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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches Telegram bot resources using the Bot API.
 * Lists recent chats (direct chats, groups, supergroups, channels) where the bot is a member.
 * Supports: chats
 */
@Component
public class TelegramResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(TelegramResourceProvider.class);
    private static final String TELEGRAM_API = "https://api.telegram.org";

    // Persistent in-memory cache of discovered chats per bot token
    private final Map<String, Map<String, ResourceOption>> tokenChatCache = new ConcurrentHashMap<>();

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
        String botToken = extractBotToken(credentials);
        if (botToken == null || botToken.isBlank()) {
            logger.warn("[telegram] No bot token provided in credentials");
            return List.of();
        }

        // If a specific search/chatId is queried, try resolving it via getChat
        if (params != null) {
            String query = params.get("search");
            if (query == null || query.isBlank()) query = params.get("query");
            if (query == null || query.isBlank()) query = params.get("chatId");
            if (query != null && !query.isBlank()) {
                fetchChatDirect(botToken, query.trim());
            }
        }

        return listChats(botToken);
    }

    private String extractBotToken(Map<String, Object> credentials) {
        if (credentials == null) return null;
        if (credentials.get("botToken") != null) return credentials.get("botToken").toString().trim();
        if (credentials.get("apiKey") != null) return credentials.get("apiKey").toString().trim();
        if (credentials.get("token") != null) return credentials.get("token").toString().trim();
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listChats(String botToken) {
        Map<String, ResourceOption> cache = tokenChatCache.computeIfAbsent(botToken, k -> new LinkedHashMap<>());

        try {
            // Include message, channel_post, my_chat_member, chat_member updates so we capture direct chats, groups, and channels
            String updateUri = TELEGRAM_API + "/bot" + botToken + "/getUpdates?limit=100&allowed_updates=%5B%22message%22%2C%22edited_message%22%2C%22channel_post%22%2C%22edited_channel_post%22%2C%22my_chat_member%22%2C%22chat_member%22%2C%22callback_query%22%5D";

            Map<String, Object> response = RestClient.builder()
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .get()
                    .uri(updateUri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null) {
                List<Map<String, Object>> result = (List<Map<String, Object>>) response.get("result");
                if (result != null && !result.isEmpty()) {
                    for (Map<String, Object> update : result) {
                        Map<String, Object> chat = extractChat(update);
                        if (chat == null || chat.get("id") == null) continue;

                        ResourceOption option = buildResourceOption(chat);
                        if (option != null) {
                            cache.put(option.id(), option);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("[telegram] Failed to fetch updates from Telegram: {}", e.getMessage());
        }

        return new ArrayList<>(cache.values());
    }

    @SuppressWarnings("unchecked")
    private void fetchChatDirect(String botToken, String chatIdOrUsername) {
        try {
            Map<String, Object> response = RestClient.builder()
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .get()
                    .uri(TELEGRAM_API + "/bot" + botToken + "/getChat?chat_id=" + chatIdOrUsername)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                Map<String, Object> chat = (Map<String, Object>) response.get("result");
                if (chat != null && chat.get("id") != null) {
                    ResourceOption option = buildResourceOption(chat);
                    if (option != null) {
                        tokenChatCache.computeIfAbsent(botToken, k -> new LinkedHashMap<>()).put(option.id(), option);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("[telegram] Could not fetch chat directly for '{}': {}", chatIdOrUsername, e.getMessage());
        }
    }

    private ResourceOption buildResourceOption(Map<String, Object> chat) {
        String chatId = String.valueOf(((Number) chat.get("id")).longValue());
        String type = chat.get("type") != null ? chat.get("type").toString().toLowerCase() : "private";
        String label;
        String description;

        if ("private".equals(type)) {
            String firstName = chat.get("first_name") != null ? chat.get("first_name").toString() : "";
            String lastName = chat.get("last_name") != null ? " " + chat.get("last_name") : "";
            String fullName = (firstName + lastName).trim();
            String username = chat.get("username") != null ? "@" + chat.get("username") : "";
            label = !fullName.isBlank() ? fullName : (!username.isBlank() ? username : "Direct Chat (" + chatId + ")");
            description = !username.isBlank() ? "Direct Chat · " + username : "Direct Chat";
        } else if ("channel".equals(type)) {
            label = chat.get("title") != null ? chat.get("title").toString() : "Channel (" + chatId + ")";
            String username = chat.get("username") != null ? "@" + chat.get("username") : "";
            description = !username.isBlank() ? "Channel · " + username : "Channel";
        } else if ("supergroup".equals(type)) {
            label = chat.get("title") != null ? chat.get("title").toString() : "Supergroup (" + chatId + ")";
            description = "Supergroup";
        } else if ("group".equals(type)) {
            label = chat.get("title") != null ? chat.get("title").toString() : "Group (" + chatId + ")";
            description = "Group";
        } else {
            label = chat.get("title") != null ? chat.get("title").toString() : chatId;
            String capitalizedType = type.isEmpty() ? "Chat" : type.substring(0, 1).toUpperCase() + type.substring(1);
            description = capitalizedType;
        }

        return new ResourceOption(chatId, label, description);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractChat(Map<String, Object> update) {
        if (update == null) return null;
        if (update.get("message") instanceof Map m && m.get("chat") instanceof Map c) return c;
        if (update.get("channel_post") instanceof Map cp && cp.get("chat") instanceof Map c) return c;
        if (update.get("my_chat_member") instanceof Map mcm && mcm.get("chat") instanceof Map c) return c;
        if (update.get("chat_member") instanceof Map cm && cm.get("chat") instanceof Map c) return c;
        if (update.get("edited_message") instanceof Map em && em.get("chat") instanceof Map c) return c;
        if (update.get("edited_channel_post") instanceof Map ecp && ecp.get("chat") instanceof Map c) return c;
        if (update.get("callback_query") instanceof Map cq && cq.get("message") instanceof Map m && m.get("chat") instanceof Map c) return c;
        return null;
    }
}
