package com.crescendo.apps.coingecko;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class CoinGeckoHandlers {

    private static final String API_BASE = "https://api.coingecko.com/api/v3";

    @Value("${crescendo.platform.coingecko-api-key:}")
    private String defaultApiKey;

    private RestClient buildClient(ActionContext context) {
        String key = null;
        if (context.credentials() != null && context.credentials().get("apiKey") != null) {
            key = context.credentials().get("apiKey").toString().trim();
        }
        if (key == null || key.isBlank()) {
            key = defaultApiKey;
        }

        RestClient.Builder builder = RestClient.builder().baseUrl(API_BASE);
        if (key != null && !key.isBlank()) {
            if (key.startsWith("CG-")) {
                builder.defaultHeader("x-cg-demo-api-key", key);
            } else {
                builder.defaultHeader("x-cg-pro-api-key", key);
            }
        }
        builder.defaultHeader("User-Agent", "Mozilla/5.0");
        return builder.build();
    }

    @ActionMapping(appKey = "coingecko", actionKey = "simple-price")
    public Object getSimplePrice(ActionContext context) throws Exception {
        String ids = context.configuration().get("ids") != null ? context.configuration().get("ids").toString() : "";
        String vsCurrencies = context.configuration().get("vsCurrencies") != null ? context.configuration().get("vsCurrencies").toString() : "usd";

        if (ids.isBlank()) {
            return ActionResult.failure("Coin IDs are required");
        }

        try {
            String response = buildClient(context)
                    .get()
                    .uri("/simple/price?ids={ids}&vs_currencies={vsCurrencies}", ids, vsCurrencies)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to fetch CoinGecko simple price: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "coingecko", actionKey = "coin-market")
    public Object getCoinMarket(ActionContext context) throws Exception {
        String id = context.configuration().get("id") != null ? context.configuration().get("id").toString() : "";
        
        if (id.isBlank()) {
            return ActionResult.failure("Coin ID is required");
        }

        try {
            String response = buildClient(context)
                    .get()
                    .uri("/coins/{id}", id)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to fetch CoinGecko coin market data: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "coingecko", actionKey = "search")
    public Object searchCoins(ActionContext context) throws Exception {
        String query = context.configuration().get("query") != null ? context.configuration().get("query").toString() : "";

        if (query.isBlank()) {
            return ActionResult.failure("Search query is required");
        }

        try {
            String response = buildClient(context)
                    .get()
                    .uri("/search?query={query}", query)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to search CoinGecko coins: " + e.getMessage());
        }
    }
}
