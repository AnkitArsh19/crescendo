package com.crescendo.apps.coingecko;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

import java.util.Map;

abstract class CoinGeckoHandler implements ActionHandler {

    private final RestClient restClient = RestClient.create("https://api.coingecko.com/api/v3");
    private final ObjectMapper objectMapper;

    CoinGeckoHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ActionResult get(String uri, Object... uriVariables) {
        try {
            String response = restClient.get()
                    .uri(uri, uriVariables)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of(
                    "data", objectMapper.readValue(response, Object.class),
                    "raw", response != null ? response : ""
            ));
        } catch (Exception e) {
            return ActionResult.failure("CoinGecko request failed: " + e.getMessage());
        }
    }

    String required(ActionContext context, String key) {
        Object value = context.configuration().get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }
}
