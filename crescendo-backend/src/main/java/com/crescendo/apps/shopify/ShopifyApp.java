package com.crescendo.apps.shopify;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ShopifyApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("shopify", "Shopify", "Manage products, orders, and customer data",
                "/icons/shopify.svg", AuthType.OAUTH2,
                List.of(Map.of(
                    "triggerKey", "new-order",
                    "name", "New Order",
                    "description", "Triggers when a new order is placed"
                )),
                List.of(Map.of(
                    "actionKey", "create-product",
                    "name", "Create Product",
                    "description", "Add a new product to your Shopify store",
                    "configSchema", Map.of(
                        "title", "string (required) — product title",
                        "price", "string (required) — price amount",
                        "description", "string — product description (HTML)"
                    )
                ))
        )
        .credentialSchema(List.of())
        .category("payments")
        .helpUrl("https://partners.shopify.com/");
    }
}
