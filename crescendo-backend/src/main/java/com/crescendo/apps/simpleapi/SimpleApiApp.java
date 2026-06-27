package com.crescendo.apps.simpleapi;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SimpleApiApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("simpleapi", "One Simple API", """
                One Simple API provides quick endpoints for generating PDFs, parsing SEO metadata, creating QR codes, exchanging currency, scraping social profiles, and more. The Crescendo SimpleAPI app provides a seamless interface to these utilities.

                **What you can do with SimpleAPI in Crescendo:**
                - Instantly generate PDF invoices from a webpage URL
                - Grab exchange rates to calculate conversions on the fly
                - Generate QR codes for marketing campaigns
                - Extract SEO meta tags and social card details from an article

                **Actions available:**
                - Execute — run any One Simple API operation directly

                **Authentication:** Requires a One Simple API token.
                """,
                "/icons/simple_api.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "execute", "name", "Execute Operation",
                        "description", "Run a One Simple API operation",
                        "configSchema", List.of(
                            Map.of("key", "resource", "label", "Resource", "type", "options", "required", true,
                                   "options", List.of(
                                       Map.of("name", "Information", "value", "information"),
                                       Map.of("name", "Social Profile", "value", "socialProfile"),
                                       Map.of("name", "Utility", "value", "utility"),
                                       Map.of("name", "Website", "value", "website")
                                   ), "default", "website"),
                            Map.of("key", "operation", "label", "Operation", "type", "options", "required", true,
                                   "options", List.of(
                                       Map.of("name", "Generate PDF", "value", "pdf"),
                                       Map.of("name", "Get SEO Data", "value", "seo"),
                                       Map.of("name", "Take Screenshot", "value", "screenshot"),
                                       Map.of("name", "Instagram Profile", "value", "instagramProfile"),
                                       Map.of("name", "Spotify Profile", "value", "spotifyProfile"),
                                       Map.of("name", "Exchange Rate", "value", "exchangeRate"),
                                       Map.of("name", "Image Metadata", "value", "imageMetadata"),
                                       Map.of("name", "Expand URL", "value", "expandURL"),
                                       Map.of("name", "Generate QR Code", "value", "qrCode")
                                   )),
                            Map.of("key", "url", "label", "URL", "type", "text", "required", false,
                                   "helpText", "The URL to process (for PDF, SEO, Screenshot, QR, Expand)"),
                            Map.of("key", "username", "label", "Username", "type", "text", "required", false,
                                   "helpText", "The social media username (for Instagram/Spotify)"),
                            Map.of("key", "baseCurrency", "label", "Base Currency", "type", "text", "required", false,
                                   "placeholder", "USD"),
                            Map.of("key", "targetCurrency", "label", "Target Currency", "type", "text", "required", false,
                                   "placeholder", "EUR")
                        ))
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiToken", "label", "API Token", "type", "password", "required", true,
                        "helpText", "Your One Simple API token")
        )).category("utility").helpUrl("https://onesimpleapi.com/");
    }
}
