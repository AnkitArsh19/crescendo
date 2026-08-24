package com.crescendo.apps.mailchimp;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class MailchimpResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(MailchimpResourceProvider.class);

    @Override
    public String appKey() {
        return "mailchimp";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("lists", "campaigns");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String apiKey = extract(credentials, "apiKey", "api_key");
        String accessToken = extract(credentials, "accessToken", "token");
        String dc = extract(credentials, "serverPrefix", "dataCenter", "dc");

        if (dc == null && apiKey != null && apiKey.contains("-")) {
            dc = apiKey.substring(apiKey.lastIndexOf("-") + 1);
        }
        if (dc == null) {
            dc = "us1";
        }

        String authHeader;
        if (apiKey != null) {
            String basic = Base64.getEncoder().encodeToString(("anystring:" + apiKey).getBytes());
            authHeader = "Basic " + basic;
        } else if (accessToken != null) {
            authHeader = "Bearer " + accessToken;
        } else {
            logger.warn("[mailchimp] Missing apiKey/accessToken for resource listing");
            return List.of();
        }

        String baseUrl = "https://" + dc + ".api.mailchimp.com/3.0";

        return switch (resourceType) {
            case "lists" -> listLists(baseUrl, authHeader);
            case "campaigns" -> listCampaigns(baseUrl, authHeader);
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

    private RestClient createClient(String baseUrl, String authHeader) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listLists(String baseUrl, String authHeader) {
        try {
            Map<String, Object> resp = createClient(baseUrl, authHeader).get()
                    .uri("/lists?count=100")
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
            logger.warn("[mailchimp] Error fetching lists: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listCampaigns(String baseUrl, String authHeader) {
        try {
            Map<String, Object> resp = createClient(baseUrl, authHeader).get()
                    .uri("/campaigns?count=100")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("campaigns") instanceof List<?> campaigns) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : campaigns) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        Map<?, ?> settings = (Map<?, ?>) item.get("settings");
                        String title = settings != null && settings.get("title") != null
                                ? String.valueOf(settings.get("title"))
                                : id;
                        options.add(new ResourceOption(id, title));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[mailchimp] Error fetching campaigns: {}", e.getMessage());
        }
        return List.of();
    }
}
