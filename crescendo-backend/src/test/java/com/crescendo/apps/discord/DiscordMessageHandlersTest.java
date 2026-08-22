package com.crescendo.apps.discord;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DiscordMessageHandlersTest {

    private final DiscordMessageHandlers handlers = new DiscordMessageHandlers();

    private ActionContext createContext(Map<String, Object> config, Map<String, Object> credentials) {
        return new ActionContext("discord", "sendMessage", config, credentials, Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    @Test
    @DisplayName("sendMessage fails when channelId is missing")
    void send_failsWhenChannelIdMissing() {
        ActionContext context = createContext(Map.of("content", "Hello Discord"), Map.of("botToken", "bot-123"));
        ActionResult result = handlers.send(context);

        assertFalse(result.success());
        assertTrue(result.error().contains("channelId"));
    }

    @Test
    @DisplayName("sendMessage fails when content is missing")
    void send_failsWhenContentMissing() {
        ActionContext context = createContext(Map.of("channelId", "1234567890"), Map.of("botToken", "bot-123"));
        ActionResult result = handlers.send(context);

        assertFalse(result.success());
        assertTrue(result.error().contains("content"));
    }
}
