package com.crescendo.apps.typeform;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class TypeformResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(TypeformResourceProvider.class);
    private static final String BASE_URL = "https://api.typeform.com";

    @Override
    public String appKey() {
        return "typeform";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("forms");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String token = extractToken(credentials);
        if (token == null || token.isBlank()) {
            logger.warn("[typeform] Missing token for resource listing");
            return List.of();
        }

        if ("forms".equals(resourceType)) {
            return listForms(token);
        }
        return List.of();
    }

    private String extractToken(Map<String, Object> credentials) {
        if (credentials == null) return null;
        if (credentials.get("apiKey") != null) return credentials.get("apiKey").toString();
        if (credentials.get("accessToken") != null) return credentials.get("accessToken").toString();
        if (credentials.get("token") != null) return credentials.get("token").toString();
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listForms(String token) {
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();

            Map<String, Object> resp = client.get()
                    .uri("/forms?page_size=100")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("items") instanceof List<?> items) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : items) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        String title = String.valueOf(item.get("title"));
                        options.add(new ResourceOption(id, title));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[typeform] Error fetching forms: {}", e.getMessage());
        }
        return List.of();
    }
}
