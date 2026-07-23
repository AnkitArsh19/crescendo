package com.crescendo.apps.notion;

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
 * Fetches Notion resources (databases, pages) via the Notion API.
 * Uses the integration token from the user's connection.
 */
@Component
@SuppressWarnings("unchecked")
public class NotionResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(NotionResourceProvider.class);
    private static final String NOTION_API = "https://api.notion.com/v1";

    private final RestClient restClient;

    public NotionResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "notion";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("databases", "pages");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("databases", 50, java.time.Duration.ofMinutes(5)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
            String resourceType,
            Map<String, String> params) {
        String token = extractToken(credentials);

        return switch (resourceType) {
            case "databases" -> listDatabases(token);
            case "pages" -> listPages(token, params != null ? params.get("databaseId") : null);
            default -> List.of();
        };
    }

    private List<ResourceOption> listDatabases(String token) {
        try {
            Map<String, Object> body = Map.of(
                    "filter", Map.of("value", "database", "property", "object"),
                    "page_size", 50);

            Map<String, Object> response = restClient.post()
                    .uri(NOTION_API + "/search")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("Notion-Version", "2022-06-28")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("results"))
                return List.of();

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

            return results.stream()
                    .map(db -> {
                        String id = String.valueOf(db.get("id"));
                        // Title is nested: title[0].plain_text
                        String title = extractTitle(db);
                        return new ResourceOption(id, title, "ID: " + id);
                    })
                    .toList();

        } catch (Exception e) {
            logger.error("[notion] Failed to list databases: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractTitle(Map<String, Object> db) {
        try {
            List<Map<String, Object>> titleArr = (List<Map<String, Object>>) db.get("title");
            if (titleArr != null && !titleArr.isEmpty()) {
                return String.valueOf(titleArr.get(0).get("plain_text"));
            }
        } catch (Exception ignored) {
        }
        return "Untitled Database";
    }

    private List<ResourceOption> listPages(String token, String databaseId) {
        if (databaseId == null || databaseId.isBlank())
            return List.of();
        try {
            Map<String, Object> body = Map.of("page_size", 50);

            Map<String, Object> response = restClient.post()
                    .uri(NOTION_API + "/databases/" + databaseId + "/query")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("Notion-Version", "2022-06-28")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("results"))
                return List.of();

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            return results.stream()
                    .map(page -> {
                        String id = String.valueOf(page.get("id"));
                        String title = extractPageTitle(page);
                        return new ResourceOption(id, title,
                                "ID: " + id.substring(0, Math.min(8, id.length())) + "...");
                    })
                    .toList();
        } catch (Exception e) {
            logger.error("[notion] Failed to list pages for database {}: {}", databaseId, e.getMessage());
            return List.of();
        }
    }

    private String extractPageTitle(Map<String, Object> page) {
        try {
            Map<String, Object> properties = (Map<String, Object>) page.get("properties");
            if (properties == null)
                return "Untitled";
            // Find the title property (any key with type 'title')
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                Map<String, Object> prop = (Map<String, Object>) entry.getValue();
                if ("title".equals(prop.get("type"))) {
                    List<Map<String, Object>> titleArr = (List<Map<String, Object>>) prop.get("title");
                    if (titleArr != null && !titleArr.isEmpty()) {
                        return String.valueOf(titleArr.get(0).get("plain_text"));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "Untitled";
    }

    private String extractToken(Map<String, Object> credentials) {
        Object token = credentials.get("accessToken");
        if (token == null)
            token = credentials.get("access_token");
        if (token == null || token.toString().isBlank()) {
            throw new IllegalArgumentException("Notion connection is missing an access token");
        }
        return token.toString();
    }
}
