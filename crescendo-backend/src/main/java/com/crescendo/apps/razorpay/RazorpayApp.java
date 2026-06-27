package com.crescendo.apps.razorpay;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RazorpayApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("razorpay", "Razorpay", """
                Razorpay is the only payments solution in India that allows businesses to accept, process, and disburse payments. The Crescendo Razorpay app lets you create and track payment links effortlessly.

                **What you can do with Razorpay in Crescendo:**
                - Generate a secure payment link immediately when a client accepts a digital proposal
                - Send an SMS reminder containing a Razorpay link for overdue invoices
                - Automatically update a Google Sheet when a new Razorpay order is created
                - Trigger a welcome email sequence in Mailchimp once a payment link is paid

                **Actions available:**
                - Create Order — generate an order ID required for standard checkout integrations
                - Create Payment Link — generate a hosted checkout URL to share with customers via email/SMS

                **Who should use this:** Indian businesses, freelancers, and e-commerce platforms automating their billing and collections.

                **Authentication:** API credentials (Key ID and Key Secret).
                """,
                "https://www.google.com/s2/favicons?domain=razorpay.com&sz=128", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "create-order", "name", "Create Order",
                                "description", "Create a Razorpay order",
                                "configSchema", List.of(
                                        Map.of("key", "amount", "label", "Amount", "type", "number", "required", true,
                                                "placeholder", "50000", "helpText", "Amount in paise"),
                                        Map.of("key", "currency", "label", "Currency", "type", "text", "required", false,
                                                "placeholder", "INR", "helpText", "Currency code"),
                                        Map.of("key", "receipt", "label", "Receipt", "type", "text", "required", false,
                                                "placeholder", "receipt_123", "helpText", "Internal receipt reference"),
                                        Map.of("key", "notes", "label", "Notes", "type", "json", "required", false,
                                                "placeholder", "{\"workflow\":\"crescendo\"}", "helpText", "Optional notes")
                                )),
                        Map.of("actionKey", "create-payment-link", "name", "Create Payment Link",
                                "description", "Create a Razorpay payment link",
                                "configSchema", List.of(
                                        Map.of("key", "amount", "label", "Amount", "type", "number", "required", true,
                                                "placeholder", "50000", "helpText", "Amount in paise"),
                                        Map.of("key", "currency", "label", "Currency", "type", "text", "required", false,
                                                "placeholder", "INR", "helpText", "Currency code"),
                                        Map.of("key", "description", "label", "Description", "type", "text", "required", false,
                                                "placeholder", "Invoice payment", "helpText", "Payment description"),
                                        Map.of("key", "customer", "label", "Customer", "type", "json", "required", false,
                                                "placeholder", "{\"name\":\"Asha\",\"email\":\"asha@example.com\"}", "helpText", "Optional customer object")
                                )),
                        Map.of("actionKey", "fetch-payment", "name", "Fetch Payment",
                                "description", "Fetch a Razorpay payment by ID",
                                "configSchema", List.of(
                                        Map.of("key", "paymentId", "label", "Payment ID", "type", "text", "required", true,
                                                "placeholder", "pay_...", "helpText", "Razorpay payment ID")
                                ))
                )
        ).credentialSchema(List.of(
                Map.of("key", "keyId", "label", "Key ID", "type", "text", "required", true,
                        "helpText", "Razorpay API key ID"),
                Map.of("key", "keySecret", "label", "Key Secret", "type", "password", "required", true,
                        "helpText", "Razorpay API key secret")
        )).category("payments").helpUrl("https://dashboard.razorpay.com/app/keys");
    }
}
