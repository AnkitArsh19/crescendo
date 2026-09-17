package com.crescendo.apps.brandfetch;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class BrandfetchApp implements AppDefinition {
    public App toApp() {
        return new App(
                "brandfetch",
                "Brandfetch", """
                Brandfetch is the global brand registry and Logo API provider. The Crescendo Brandfetch app allows you to instantly retrieve accurate, high-resolution company logos and brand assets into your workflows.

                **What you can do with Brandfetch in Crescendo:**
                - Fetch high-res vector and raster logos for any company domain via Brandfetch Logo API
                - Search for brands and companies by name to resolve domains and logos
                - Dynamically embed partner or customer logos into emails, documents, PDFs, and dashboards
                - Retrieve brand profiles with colors and assets

                **Actions available:**
                - Get Logo — Retrieve direct CDN logo URL (png/svg, light/dark, icon/symbol) for any company domain via Brandfetch Logo API
                - Search Brands — Search companies by name to retrieve brand metadata, domains, and logos
                - Get Brand — Fetch comprehensive brand assets and profile for a company domain

                **Authentication:** Uses Crescendo platform client ID by default. Custom Client ID or API Key can also be provided in Connections for custom limits.
                """,
                "https://www.google.com/s2/favicons?domain=brandfetch.com&sz=128",
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "get-logo",
                                "name", "Get Logo",
                                "description", "Retrieve direct CDN logo URL for any company domain via Brandfetch Logo API",
                                "configSchema", List.of(
                                        Map.of("key", "domain", "label", "Company Domain", "type", "text", "required", true, "placeholder", "apple.com"),
                                        Map.of("key", "type", "label", "Asset Type", "type", "dropdown", "required", false, "default", "logo", "options", List.of("logo", "icon", "symbol")),
                                        Map.of("key", "theme", "label", "Theme", "type", "dropdown", "required", false, "options", List.of("light", "dark")),
                                        Map.of("key", "format", "label", "Format", "type", "dropdown", "required", false, "options", List.of("png", "svg"))
                                )
                        ),
                        Map.of(
                                "actionKey", "search-brand",
                                "name", "Search Brands",
                                "description", "Search companies by name to retrieve domains and logos",
                                "configSchema", List.of(
                                        Map.of("key", "query", "label", "Brand Name", "type", "text", "required", true, "placeholder", "Apple")
                                )
                        ),
                        Map.of(
                                "actionKey", "get-brand",
                                "name", "Get Brand Profile",
                                "description", "Fetch brand profile and logo assets by domain",
                                "configSchema", List.of(
                                        Map.of("key", "domain", "label", "Domain", "type", "text", "required", true, "placeholder", "openai.com")
                                )
                        )
                )
        )
        .hasPlatformKey(true)
        .credentialSchema(List.of(
                Map.of("key", "clientId", "label", "Brandfetch Client ID", "type", "text", "required", false, "placeholder", "Optional Client ID for Logo API"),
                Map.of("key", "apiKey", "label", "Brandfetch API Key", "type", "password", "required", false, "placeholder", "Optional API Key for Brand API")
        ))
        .category("data")
        .helpUrl("https://docs.brandfetch.com/");
    }
}
