package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SlackMessageHandlersTest {

    private final SlackMessageHandlers handlers = new SlackMessageHandlers();

    private ActionContext createContext(Map<String, Object> config, Map<String, Object> credentials) {
        return new ActionContext("slack", "sendMessage", config, credentials, Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    @Test
    @DisplayName("sendMessage fails when channel is missing")
    void send_failsWhenChannelMissing() {
        ActionContext context = createContext(Map.of("text", "Hello Slack"), Map.of("accessToken", "xoxb-test"));
        ActionResult result = handlers.send(context);

        assertFalse(result.success());
        assertTrue(result.error().contains("channel"));
    }

    @Test
    @DisplayName("sendMessage fails when text and blocks are both missing")
    void send_failsWhenTextAndBlocksMissing() {
        ActionContext context = createContext(Map.of("channel", "C12345"), Map.of("accessToken", "xoxb-test"));
        ActionResult result = handlers.send(context);

        assertFalse(result.success());
        assertTrue(result.error().contains("Either 'text' or 'blocksUi' is required"));
    }
}
