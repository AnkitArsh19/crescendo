package com.crescendo.apps.youtube;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class YouTubeSupport {

    public static String resolveToken(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        return token != null && !token.toString().isBlank() ? token.toString() : null;
    }

    public static String resolveApiKey(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (creds == null) return null;
        Object key = creds.get("apiKey");
        return key != null && !key.toString().isBlank() ? key.toString() : null;
    }

    public static ActionResult missingAuth() {
        return ActionResult.failure("YouTube requires either an 'accessToken' or 'apiKey' in connection credentials");
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
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return builder;
    }
}
