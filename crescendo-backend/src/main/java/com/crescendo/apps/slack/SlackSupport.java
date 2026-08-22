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

    @SuppressWarnings("unchecked")
    public static ActionResult parseSlackResponse(String responseStr, Map<String, Object> additionalOutput) {
        if (responseStr == null || responseStr.isBlank()) {
            return ActionResult.failure("Empty response from Slack");
        }
        try {
            Map<String, Object> respMap = mapper.readValue(responseStr, Map.class);
            if (Boolean.FALSE.equals(respMap.get("ok"))) {
                String error = String.valueOf(respMap.get("error"));
                String humanError = switch (error) {
                    case "not_in_channel" -> "The Slack app/bot is not a member of the selected channel. Please go to the channel in Slack and type '/invite @Crescendo' (or your bot's name) to add it.";
                    case "channel_not_found" -> "Slack channel not found. Please verify the channel ID or name, and ensure the app has access.";
                    case "is_archived" -> "The specified Slack channel is archived.";
                    case "msg_too_long" -> "Message text exceeds Slack's maximum character limit.";
                    case "invalid_auth", "account_inactive", "token_revoked" -> "Slack authentication expired or invalid. Please reconnect your Slack account in Crescendo.";
                    case "ratelimited" -> "Slack rate limit reached. Please wait a moment before sending more messages.";
                    default -> "Slack API returned error: '" + error + "'";
                };
                return ActionResult.failure(humanError);
            }
            Map<String, Object> output = new java.util.HashMap<>(respMap);
            if (additionalOutput != null) {
                output.putAll(additionalOutput);
            }
            return ActionResult.success(output);
        } catch (Exception e) {
            Map<String, Object> output = new java.util.HashMap<>();
            if (additionalOutput != null) output.putAll(additionalOutput);
            output.put("response", responseStr);
            return ActionResult.success(output);
        }
    }
}
