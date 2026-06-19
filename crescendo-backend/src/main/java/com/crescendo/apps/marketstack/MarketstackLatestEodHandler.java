package com.crescendo.apps.marketstack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

import java.util.Map;

@ActionMapping(appKey = "marketstack", actionKey = "latest-eod")
public class MarketstackLatestEodHandler implements ActionHandler {

    private final ObjectMapper objectMapper;

    public MarketstackLatestEodHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ActionResult execute(ActionContext context) {
        try {
            String symbols = value(context.configuration(), "symbols", "");
            if (symbols.isBlank())
                return ActionResult.failure("Marketstack symbols are required");
            int limit = intValue(context.configuration().get("limit"), 10);
            String response = RestClient.create("https://api.marketstack.com/v1")
                    .get()
                    .uri("/eod/latest?access_key={key}&symbols={symbols}&limit={limit}",
                            value(context.credentials(), "accessKey", ""), symbols, Math.max(1, limit))
                    .retrieve()
                    .body(String.class);
            return ActionResult
                    .success(Map.of("data", objectMapper.readValue(response, Object.class), "raw", response));
        } catch (Exception e) {
            return ActionResult.failure("Marketstack latest EOD failed: " + e.getMessage());
        }
    }

    private String value(Map<String, Object> map, String key, String fallback) {
        Object value = map != null ? map.get(key) : null;
        return value == null ? fallback : String.valueOf(value);
    }

    private int intValue(Object value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
