package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class SlackSupport {

    public static final String SLACK_API = "https://slack.com/api/";
    private static final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    public static String resolveToken(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        if (token == null) token = creds.get("botToken");
        if (token == null) token = creds.get("apiKey");
        return token != null && !token.toString().isBlank() ? token.toString() : null;
    }

    public static ActionResult missingToken() {
        return ActionResult.failure("Slack requires a 'botToken' or 'accessToken' in connection credentials");
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

    public static tools.jackson.databind.ObjectMapper getMapper() {
        return mapper;
    }
}
