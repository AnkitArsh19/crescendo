package com.crescendo.apps.mattermost;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class MattermostSupport {

    private static final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    public static String resolveToken(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        return token != null && !token.toString().isBlank() ? token.toString() : null;
    }

    public static String getBaseUrl(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (creds == null) return null;
        Object baseUrl = creds.get("baseUrl");
        if (baseUrl != null && !baseUrl.toString().isBlank()) {
            String url = baseUrl.toString();
            return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        }
        return null;
    }

    public static ActionResult missingCredentials() {
        return ActionResult.failure("Mattermost requires 'accessToken' and 'baseUrl' in connection credentials");
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
        if (v == null || v.toString().isBlank()) return defaultVal;
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    public static RestClient.Builder clientBuilder(ActionContext context) {
        String token = resolveToken(context);
        String baseUrl = getBaseUrl(context);
        
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                
        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }
        if (token != null) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return builder;
    }

    public static tools.jackson.databind.ObjectMapper getMapper() {
        return mapper;
    }
}
