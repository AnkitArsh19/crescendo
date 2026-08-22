package com.crescendo.apps.discord;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

@org.springframework.stereotype.Component
public class DiscordSupport {

    public static final String DISCORD_API = "https://discord.com/api/v10/";
    private static final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
    private static com.crescendo.connections.oauth.IntegrationOAuthConfig oauthConfig;

    public DiscordSupport(com.crescendo.connections.oauth.IntegrationOAuthConfig config) {
        DiscordSupport.oauthConfig = config;
    }

    public static String resolveToken(ActionContext context) {
        Map<String, Object> credentials = context.credentials();
        if (credentials != null) {
            Object botToken = credentials.get("botToken");
            if (botToken != null && !botToken.toString().isBlank()) {
                return "Bot " + botToken.toString();
            }
            Object apiKey = credentials.get("apiKey");
            if (apiKey != null && !apiKey.toString().isBlank()) {
                return "Bot " + apiKey.toString();
            }
        }
        // Fallback to platform-configured bot token
        if (oauthConfig != null && oauthConfig.hasProvider("discord")) {
            String platformBotToken = oauthConfig.getProvider("discord").getBotToken();
            if (platformBotToken != null && !platformBotToken.isBlank()) {
                return "Bot " + platformBotToken;
            }
        }
        if (credentials != null) {
            Object accessToken = credentials.get("accessToken");
            if (accessToken != null && !accessToken.toString().isBlank()) {
                return "Bearer " + accessToken.toString();
            }
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

    public static ActionResult handleDiscordError(String action, Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("401") || msg.contains("403") || msg.contains("Unauthorized") || msg.contains("Forbidden")) {
            return ActionResult.failure("Discord " + action + " failed (401/403 Forbidden). Discord requires a Bot Token ('botToken') with proper channel permissions. Personal user OAuth tokens cannot post messages or list channels. Please connect using your Discord Bot Token.");
        }
        return ActionResult.failure("Discord " + action + " failed: " + msg);
    }
}
