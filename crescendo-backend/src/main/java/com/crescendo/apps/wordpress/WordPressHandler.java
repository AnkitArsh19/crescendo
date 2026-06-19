package com.crescendo.apps.wordpress;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

abstract class WordPressHandler implements ActionHandler {

    private final ObjectMapper objectMapper;

    WordPressHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ActionResult get(ActionContext context, String uri, Object... vars) {
        try {
            String response = client(context).get().uri(uri, vars).retrieve().body(String.class);
            return parsed(response);
        } catch (Exception e) {
            return ActionResult.failure("WordPress request failed: " + e.getMessage());
        }
    }

    ActionResult post(ActionContext context, Object body) {
        try {
            String response = client(context).post()
                    .uri("/wp-json/wp/v2/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parsed(response);
        } catch (Exception e) {
            return ActionResult.failure("WordPress request failed: " + e.getMessage());
        }
    }

    String value(ActionContext context, String key, String fallback) {
        Object value = context.configuration().get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private RestClient client(ActionContext context) {
        String siteUrl = credential(context, "siteUrl");
        String basic = credential(context, "username") + ":" + credential(context, "applicationPassword");
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(siteUrl))
                .defaultHeader(HttpHeaders.AUTHORIZATION,
                        "Basic " + Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8)))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private ActionResult parsed(String response) throws Exception {
        return ActionResult.success(Map.of(
                "data", response != null && !response.isBlank() ? objectMapper.readValue(response, Object.class) : Map.of(),
                "raw", response != null ? response : ""
        ));
    }

    private String credential(ActionContext context, String key) {
        Object value = context.credentials() != null ? context.credentials().get(key) : null;
        return value == null ? "" : String.valueOf(value);
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
