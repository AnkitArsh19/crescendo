package com.crescendo.apps.shopify;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "shopify", actionKey = "create-product")
public class ShopifyCreateProductHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ShopifyCreateProductHandler.class);

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

        String title = config.get("title") != null ? config.get("title").toString() : null;
        if (title == null || title.isBlank()) return ActionResult.failure("'title' is required");

        try {
            Map<String, Object> product = new HashMap<>();
            product.put("title", title);
            if (config.containsKey("bodyHtml")) product.put("body_html", config.get("bodyHtml").toString());
            if (config.containsKey("vendor")) product.put("vendor", config.get("vendor").toString());
            if (config.containsKey("productType")) product.put("product_type", config.get("productType").toString());

            String response = RestClient.create()
                    .post()
                    .uri("https://" + shopDomain + "/admin/api/2024-01/products.json")
                    .header("X-Shopify-Access-Token", accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("product", product))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[shopify] Product created successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[shopify] Create product failed", e);
            return ActionResult.failure("Shopify create product failed: " + e.getMessage());
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
