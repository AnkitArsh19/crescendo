package com.crescendo.apps.brevo;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class BrevoResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(BrevoResourceProvider.class);
    private static final String BASE_URL = "https://api.brevo.com/v3";

    @Override
    public String appKey() {
        return "brevo";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("templates", "senders", "lists");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String apiKey = extract(credentials, "apiKey", "api-key", "token");
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("[brevo] Missing apiKey for resource listing");
            return List.of();
        }

        return switch (resourceType) {
            case "templates" -> listTemplates(apiKey);
            case "senders" -> listSenders(apiKey);
            case "lists" -> listLists(apiKey);
            default -> List.of();
        };
    }

    private String extract(Map<String, Object> credentials, String... keys) {
        if (credentials == null) return null;
        for (String k : keys) {
            Object v = credentials.get(k);
            if (v != null && !v.toString().isBlank()) return v.toString();
        }
        return null;
    }

    private RestClient createClient(String apiKey) {
        return RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("api-key", apiKey)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listTemplates(String apiKey) {
        try {
            Map<String, Object> resp = createClient(apiKey).get()
                    .uri("/smtp/templates?templateStatus=true&limit=100")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("templates") instanceof List<?> templates) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : templates) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        String name = String.valueOf(item.get("name"));
                        String tag = item.get("tag") != null ? String.valueOf(item.get("tag")) : null;
                        options.add(new ResourceOption(id, name, tag));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[brevo] Error fetching templates: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listSenders(String apiKey) {
        try {
            Map<String, Object> resp = createClient(apiKey).get()
                    .uri("/senders")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("senders") instanceof List<?> senders) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : senders) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        String name = String.valueOf(item.get("name"));
                        String email = String.valueOf(item.get("email"));
                        options.add(new ResourceOption(id, name + " <" + email + ">"));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[brevo] Error fetching senders: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listLists(String apiKey) {
        try {
            Map<String, Object> resp = createClient(apiKey).get()
                    .uri("/contacts/lists?limit=50")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("lists") instanceof List<?> lists) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : lists) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        String name = String.valueOf(item.get("name"));
                        options.add(new ResourceOption(id, name));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[brevo] Error fetching contact lists: {}", e.getMessage());
        }
        return List.of();
    }
}
