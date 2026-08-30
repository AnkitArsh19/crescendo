package com.crescendo.apps.razorpay;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Razorpay resources: recent orders and payments.
 * Authenticates via HTTP Basic with keyId:keySecret.
 */
@Component
@SuppressWarnings("unchecked")
public class RazorpayResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayResourceProvider.class);
    private static final String RAZORPAY_API = "https://api.razorpay.com/v1";

    private final RestClient restClient;

    public RazorpayResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "razorpay";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("orders", "payments");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of();
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String keyId = str(credentials.get("keyId"));
        String keySecret = str(credentials.get("keySecret"));
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new IllegalArgumentException("Razorpay requires keyId and keySecret.");
        }
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes());

        return switch (resourceType) {
            case "orders"   -> listOrders(basicAuth);
            case "payments" -> listPayments(basicAuth);
            default -> List.of();
        };
    }

    private List<ResourceOption> listOrders(String auth) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(RAZORPAY_API + "/orders?count=50")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(Map.class);
            if (resp == null || !resp.containsKey("items")) return List.of();
            List<Map<String, Object>> items = (List<Map<String, Object>>) resp.get("items");
            if (items == null) return List.of();

            return items.stream().map(o -> {
                String id = str(o.get("id"));
                Object amount = o.get("amount");
                String currency = str(o.get("currency"));
                String status = str(o.get("status"));
                double val = amount instanceof Number ? ((Number) amount).doubleValue() / 100.0 : 0.0;
                return new ResourceOption(id, id + " (" + currency + " " + val + ")", "Status: " + status);
            }).toList();
        } catch (Exception e) {
            logger.error("[razorpay] Failed to list orders: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listPayments(String auth) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(RAZORPAY_API + "/payments?count=50")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(Map.class);
            if (resp == null || !resp.containsKey("items")) return List.of();
            List<Map<String, Object>> items = (List<Map<String, Object>>) resp.get("items");
            if (items == null) return List.of();

            return items.stream().map(p -> {
                String id = str(p.get("id"));
                Object amount = p.get("amount");
                String currency = str(p.get("currency"));
                String email = str(p.get("email"));
                String status = str(p.get("status"));
                double val = amount instanceof Number ? ((Number) amount).doubleValue() / 100.0 : 0.0;
                return new ResourceOption(id, id + " (" + currency + " " + val + (email.isBlank() ? "" : " - " + email) + ")", "Status: " + status);
            }).toList();
        } catch (Exception e) {
            logger.error("[razorpay] Failed to list payments: {}", e.getMessage());
            return List.of();
        }
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
