package com.crescendo.apps.shopify;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "shopify", actionKey = "get-orders")
public class ShopifyGetOrdersHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ShopifyGetOrdersHandler.class);

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = readCredential(creds, "accessToken", "apiKey");
        String shopDomain = creds != null ? (String) creds.get("shopDomain") : null;

        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Shopify requires 'accessToken' in connection credentials");
        }
        if (shopDomain == null || shopDomain.isBlank()) return ActionResult.failure("Shopify requires 'shopDomain'");

        String status = config.getOrDefault("status", "any").toString();
        int limit = 50;
        if (config.containsKey("limit")) {
            try { limit = Integer.parseInt(config.get("limit").toString()); }
            catch (NumberFormatException ignored) {}
        }

        try {
            String uri = "https://" + shopDomain + "/admin/api/2024-01/orders.json?status=" + status + "&limit=" + limit;

            String response = RestClient.create()
                    .get()
                    .uri(uri)
                    .header("X-Shopify-Access-Token", accessToken)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[shopify] Orders fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[shopify] Get orders failed", e);
            return ActionResult.failure("Shopify get orders failed: " + e.getMessage());
        }
    }

    private String readCredential(Map<String, Object> creds, String... keys) {
        if (creds == null) return null;
        for (String key : keys) {
            Object value = creds.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }
}
