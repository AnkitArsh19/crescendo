package com.crescendo.apps.brandfetch;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BrandfetchHandlers {

    private static final Logger log = LoggerFactory.getLogger(BrandfetchHandlers.class);

    @Value("${crescendo.platform.brandfetch-client-id:${BRANDFETCH_CLIENT_ID:1idpV51hVNVnpZYeWf4}}")
    private String defaultClientId;

    @Value("${crescendo.platform.brandfetch-api-key:${BRANDFETCH_API_KEY:}}")
    private String defaultApiKey;

    public BrandfetchHandlers() {
        this.defaultClientId = "1idpV51hVNVnpZYeWf4";
        this.defaultApiKey = "";
    }

    public BrandfetchHandlers(String defaultClientId, String defaultApiKey) {
        this.defaultClientId = defaultClientId != null && !defaultClientId.isBlank() ? defaultClientId : "1idpV51hVNVnpZYeWf4";
        this.defaultApiKey = defaultApiKey != null ? defaultApiKey : "";
    }

    /**
     * Get Logo via Brandfetch Logo API (cdn.brandfetch.io).
     * Retrieves direct CDN URL for company logo, icon, or symbol.
     */
    @ActionMapping(appKey = "brandfetch", actionKey = "get-logo")
    public Object getLogo(ActionContext context) throws Exception {
        String rawDomain = context.configuration().get("domain") != null ? context.configuration().get("domain").toString() : "";
        if (rawDomain.isBlank()) {
            return ActionResult.failure("Brandfetch domain is required");
        }

        String domain = sanitizeDomain(rawDomain);
        String type = context.configuration().getOrDefault("type", "logo").toString().toLowerCase().trim();
        String theme = context.configuration().get("theme") != null ? context.configuration().get("theme").toString().toLowerCase().trim() : "";
        String format = context.configuration().get("format") != null ? context.configuration().get("format").toString().toLowerCase().trim() : "";

        String clientId = resolveClientId(context);

        // Construct Brandfetch Logo API path
        StringBuilder pathBuilder = new StringBuilder("https://cdn.brandfetch.io/").append(domain);
        if ("icon".equals(type)) {
            pathBuilder.append("/icon");
        } else if ("symbol".equals(type)) {
            pathBuilder.append("/symbol");
        }

        pathBuilder.append("?c=").append(clientId);
        if (!theme.isBlank()) {
            pathBuilder.append("&theme=").append(URLEncoder.encode(theme, StandardCharsets.UTF_8));
        }
        if (!format.isBlank()) {
            pathBuilder.append("&format=").append(URLEncoder.encode(format, StandardCharsets.UTF_8));
        }

        String selectedUrl = pathBuilder.toString();
        String baseLogoUrl = "https://cdn.brandfetch.io/" + domain + "?c=" + clientId;
        String iconUrl = "https://cdn.brandfetch.io/" + domain + "/icon?c=" + clientId;
        String symbolUrl = "https://cdn.brandfetch.io/" + domain + "/symbol?c=" + clientId;
        String htmlSnippet = "<img src=\"" + selectedUrl + "\" alt=\"" + domain + " logo\" />";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("logoUrl", selectedUrl);
        result.put("primaryLogoUrl", baseLogoUrl);
        result.put("iconUrl", iconUrl);
        result.put("symbolUrl", symbolUrl);
        result.put("domain", domain);
        result.put("html", htmlSnippet);
        result.put("clientId", clientId);

        return ActionResult.success(result);
    }

    /**
     * Search brands by name using Brandfetch Search API.
     */
    @ActionMapping(appKey = "brandfetch", actionKey = "search-brand")
    public Object searchBrand(ActionContext context) throws Exception {
        String query = context.configuration().get("query") != null ? context.configuration().get("query").toString().trim() : "";
        if (query.isBlank()) {
            return ActionResult.failure("Brand search query is required");
        }

        String clientId = resolveClientId(context);

        try {
            String response = RestClient.create("https://api.brandfetch.io/v2")
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/{query}")
                            .queryParam("c", clientId)
                            .build(query))
                    .retrieve()
                    .body(String.class);

            return ActionResult.success(Map.of("query", query, "results", response != null ? response : "[]"));
        } catch (Exception e) {
            log.warn("Brandfetch search failed for query='{}': {}", query, e.getMessage());
            return ActionResult.failure("Brandfetch search failed: " + e.getMessage());
        }
    }

    /**
     * Get Brand Profile. Calls Brandfetch Brand API if key available,
     * or gracefully resolves Logo API CDN bundle.
     */
    @ActionMapping(appKey = "brandfetch", actionKey = "get-brand")
    public Object getBrand(ActionContext context) throws Exception {
        String rawDomain = context.configuration().get("domain") != null ? context.configuration().get("domain").toString() : "";
        if (rawDomain.isBlank()) {
            return ActionResult.failure("Brandfetch domain is required");
        }

        String domain = sanitizeDomain(rawDomain);
        String apiKey = resolveApiKey(context);
        String clientId = resolveClientId(context);

        // Try calling the full Brand API if an API key is present
        if (!apiKey.isBlank()) {
            try {
                String response = RestClient.create("https://api.brandfetch.io/v2")
                        .get()
                        .uri("/brands/{domain}", domain)
                        .header("Authorization", "Bearer " + apiKey)
                        .retrieve()
                        .body(String.class);

                return ActionResult.success(Map.of("domain", domain, "data", response));
            } catch (Exception e) {
                log.warn("Brandfetch Brand API call failed for domain '{}', falling back to Logo API: {}", domain, e.getMessage());
            }
        }

        // Fallback: provide rich Logo API asset bundle
        String logoUrl = "https://cdn.brandfetch.io/" + domain + "?c=" + clientId;
        String iconUrl = "https://cdn.brandfetch.io/" + domain + "/icon?c=" + clientId;
        String symbolUrl = "https://cdn.brandfetch.io/" + domain + "/symbol?c=" + clientId;

        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("domain", domain);
        fallback.put("logoUrl", logoUrl);
        fallback.put("iconUrl", iconUrl);
        fallback.put("symbolUrl", symbolUrl);
        fallback.put("html", "<img src=\"" + logoUrl + "\" alt=\"" + domain + " logo\" />");
        fallback.put("data", Map.of(
                "domain", domain,
                "logo", logoUrl,
                "icon", iconUrl,
                "symbol", symbolUrl
        ));

        return ActionResult.success(fallback);
    }

    private String resolveClientId(ActionContext context) {
        if (context.credentials() != null) {
            Object cid = context.credentials().get("clientId");
            if (cid != null && !cid.toString().isBlank()) {
                return cid.toString().trim();
            }
        }
        return (defaultClientId != null && !defaultClientId.isBlank()) ? defaultClientId.trim() : "1idpV51hVNVnpZYeWf4";
    }

    private String resolveApiKey(ActionContext context) {
        if (context.credentials() != null) {
            Object key = context.credentials().get("apiKey");
            if (key != null && !key.toString().isBlank()) {
                return key.toString().trim();
            }
        }
        return (defaultApiKey != null) ? defaultApiKey.trim() : "";
    }

    private String sanitizeDomain(String input) {
        String clean = input.trim();
        clean = clean.replaceAll("^(https?://)?(www\\.)?", "");
        int slashIdx = clean.indexOf('/');
        if (slashIdx != -1) {
            clean = clean.substring(0, slashIdx);
        }
        return clean.toLowerCase();
    }
}
