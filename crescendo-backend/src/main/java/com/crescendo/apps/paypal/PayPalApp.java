package com.crescendo.apps.paypal;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PayPalApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("paypal", "PayPal", """
                PayPal is a global leader in online payments. The Crescendo PayPal app lets you automate invoicing and track checkout orders seamlessly within your workflows.

                **What you can do with PayPal in Crescendo:**
                - Automatically generate a PayPal checkout link when a user clicks "Buy" in your custom application
                - Send a confirmation email via SendGrid when a PayPal order is successfully captured
                - Retrieve order details to update inventory counts in your PostgreSQL database
                - Notify your accounting channel in Slack when a large payment clears

                **Actions available:**
                - Create Order — generate a checkout session for a specific currency and value
                - Get Order — retrieve the current status and details of an existing order

                **Who should use this:** E-commerce operators, freelancers, and billing teams integrating digital payments.

                **Authentication:** API credentials (Client ID and Secret).
                """,
                "https://www.google.com/s2/favicons?domain=paypal.com&sz=128", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "paypal:order:create", "name", "Create Order",
                                "description", "Create a PayPal checkout order",
                                "configSchema", List.of(
                                        Map.of("key", "amount", "label", "Amount", "type", "text", "required", true,
                                                "placeholder", "10.00", "helpText", "Decimal amount"),
                                        Map.of("key", "currency", "label", "Currency", "type", "text", "required", false,
                                                "placeholder", "USD", "helpText", "Currency code"),
                                        Map.of("key", "intent", "label", "Intent", "type", "text", "required", false,
                                                "placeholder", "CAPTURE", "helpText", "CAPTURE or AUTHORIZE")
                                )),
                        Map.of("actionKey", "paypal:order:get", "name", "Fetch Order",
                                "description", "Fetch a PayPal order",
                                "configSchema", List.of(
                                        Map.of("key", "orderId", "label", "Order ID", "type", "text", "required", true,
                                                "placeholder", "5O190127TN364715T", "helpText", "PayPal order ID")
                                ))
                )
        ).credentialSchema(List.of(
                Map.of("key", "clientId", "label", "Client ID", "type", "text", "required", true,
                        "helpText", "PayPal REST app client ID"),
                Map.of("key", "clientSecret", "label", "Client Secret", "type", "password", "required", true,
                        "helpText", "PayPal REST app client secret"),
                Map.of("key", "environment", "label", "Environment", "type", "select", "required", false,
                        "options", List.of(Map.of("value", "sandbox", "label", "Sandbox"), Map.of("value", "live", "label", "Live")),
                        "helpText", "Use sandbox for free testing")
        )).category("payments").helpUrl("https://developer.paypal.com/dashboard/applications/");
    }
}
