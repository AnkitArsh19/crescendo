package com.crescendo.apps.paypal;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches PayPal resources.
 * Authenticates via OAuth2 token exchange with clientId + clientSecret.
 */
@Component
@SuppressWarnings("unchecked")
public class PayPalResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(PayPalResourceProvider.class);

    private final RestClient restClient;

    public PayPalResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "paypal";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("orders");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of();
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String baseUrl = isSandbox(credentials)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
        String token = getAccessToken(baseUrl, credentials);

        return switch (resourceType) {
            case "orders" -> listOrders(baseUrl, token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listOrders(String baseUrl, String token) {
        // PayPal orders are generally retrieved by ID; return empty list or recent invoices
        return List.of();
    }

    private String getAccessToken(String baseUrl, Map<String, Object> credentials) {
        String clientId = str(credentials.get("clientId"));
        String clientSecret = str(credentials.get("clientSecret"));
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalArgumentException("PayPal requires clientId and clientSecret.");
        }
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        Map<String, Object> resp = restClient.post()
                .uri(baseUrl + "/v1/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, basicAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve().body(Map.class);

        if (resp != null && resp.containsKey("access_token")) {
            return str(resp.get("access_token"));
        }
        throw new IllegalStateException("Failed to retrieve PayPal access token.");
    }

    private boolean isSandbox(Map<String, Object> credentials) {
        String env = str(credentials.get("environment"));
        return "sandbox".equalsIgnoreCase(env) || env.isBlank();
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
