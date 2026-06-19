package com.crescendo.apps.paypal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@SuppressWarnings("unchecked")
final class PayPalClient {
    private PayPalClient() {
    }

    static String baseUrl(Map<String, Object> credentials) {
        return "live".equalsIgnoreCase(String.valueOf(credentials.getOrDefault("environment", "sandbox")))
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";
    }

    static String accessToken(Map<String, Object> credentials) {
        String raw = credentials.get("clientId") + ":" + credentials.get("clientSecret");
        String basic = "Basic " + java.util.Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> response = RestClient.builder().baseUrl(baseUrl(credentials)).build()
                .post()
                .uri("/v1/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, basic)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(Map.class);
        return response == null ? null : String.valueOf(response.get("access_token"));
    }
}
