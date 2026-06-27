package com.crescendo.apps.discord;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class DiscordSupport {

    public static final String DISCORD_API = "https://discord.com/api/v10/";
    private static final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    public static String resolveToken(ActionContext context) {
        Map<String, Object> credentials = context.credentials();
        if (credentials == null) return null;
        Object botToken = credentials.get("botToken");
        if (botToken != null && !botToken.toString().isBlank()) {
            return "Bot " + botToken.toString();
        }
        Object apiKey = credentials.get("apiKey");
        if (apiKey != null && !apiKey.toString().isBlank()) {
            return "Bot " + apiKey.toString();
        }
        Object accessToken = credentials.get("accessToken");
        if (accessToken != null && !accessToken.toString().isBlank()) {
            return "Bearer " + accessToken.toString();
        }
        return null;
    }

    public static ActionResult missingToken() {
        return ActionResult.failure("Discord requires a 'botToken' or 'accessToken' in connection credentials");
    }

    public static String require(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : null;
    }

    public static String opt(Map<String, Object> config, String key, String defaultVal) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : defaultVal;
    }

    public static int parseIntOpt(Map<String, Object> config, String key, int defaultVal) {
        Object v = config.get(key);
        if (v == null) return defaultVal;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return defaultVal; }
    }

    public static RestClient.Builder clientBuilder(ActionContext context) {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        String token = resolveToken(context);
        if (token != null) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, token);
        }
        return builder;
    }

    public static tools.jackson.databind.ObjectMapper getMapper() {
        return mapper;
    }
}
