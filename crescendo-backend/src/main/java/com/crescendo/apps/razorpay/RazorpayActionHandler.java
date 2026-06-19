package com.crescendo.apps.razorpay;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@ActionMapping(appKey = "razorpay", actionKey = "create-order")
public class RazorpayActionHandler implements ActionHandler {
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.razorpay.com/v1")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();

    @Override
public ActionResult execute(ActionContext context) {
        return post(context, "/orders", orderBody(context.configuration()));
    }

    ActionResult post(ActionContext context, String path, Map<String, Object> body) {
        try {
            String response = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, basic(context.credentials()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("body", response));
        } catch (Exception e) {
            return ActionResult.failure("Razorpay request failed: " + e.getMessage());
        }
    }

    static Map<String, Object> orderBody(Map<String, Object> config) {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", config.get("amount"));
        body.put("currency", config.getOrDefault("currency", "INR"));
        if (config.get("receipt") != null) body.put("receipt", config.get("receipt"));
        if (config.get("notes") != null) body.put("notes", config.get("notes"));
        return body;
    }

    static String basic(Map<String, Object> credentials) {
        String raw = credentials.get("keyId") + ":" + credentials.get("keySecret");
        return "Basic " + java.util.Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
