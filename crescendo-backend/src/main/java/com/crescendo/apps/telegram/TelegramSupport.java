package com.crescendo.apps.telegram;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;

import java.util.Map;

public class TelegramSupport {

    public static final String TELEGRAM_API = "https://api.telegram.org/bot";
    private static final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    public static String resolveToken(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (creds == null) return null;
        Object token = creds.get("apiKey");
        return token != null && !token.toString().isBlank() ? token.toString() : null;
    }

    public static ActionResult missingToken() {
        return ActionResult.failure("Telegram requires 'apiKey' (bot token) in connection credentials");
    }

    public static String require(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : null;
    }

    public static String opt(Map<String, Object> config, String key, String defaultVal) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : defaultVal;
    }

    public static tools.jackson.databind.ObjectMapper getMapper() {
        return mapper;
    }
}
