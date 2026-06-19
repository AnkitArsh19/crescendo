package com.crescendo.apps.simpleapi;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public final class SimpleApiSupport {
    private SimpleApiSupport() {}

    public static RestClient bearer(String baseUrl, String token) {
        return RestClient.builder().baseUrl(trim(baseUrl))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build();
    }

    public static RestClient basic(String baseUrl, String user, String pass) {
        String raw = user + ":" + pass;
        return RestClient.builder().baseUrl(trim(baseUrl))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build();
    }

    public static ActionResult parsed(ObjectMapper mapper, String response) throws Exception {
        return ActionResult.success(Map.of("data", response != null && !response.isBlank() ? mapper.readValue(response, Object.class) : Map.of(), "raw", response != null ? response : ""));
    }

    public static String cfg(ActionContext c, String key) {
        Object value = c.configuration().get(key);
        return value == null ? "" : String.valueOf(value);
    }

    public static String cred(ActionContext c, String key) {
        Object value = c.credentials() != null ? c.credentials().get(key) : null;
        return value == null ? "" : String.valueOf(value);
    }

    public static int intCfg(ActionContext c, String key, int fallback) {
        try { String value = cfg(c, key); return value.isBlank() ? fallback : Integer.parseInt(value); }
        catch (Exception e) { return fallback; }
    }

    public static String trim(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
