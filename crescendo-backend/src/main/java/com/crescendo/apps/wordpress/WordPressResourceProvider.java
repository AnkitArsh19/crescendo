package com.crescendo.apps.wordpress;

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
 * Fetches WordPress resources: posts, pages, categories, tags, users, media.
 * Authenticates via Application Password (HTTP Basic: username + applicationPassword).
 */
@Component
@SuppressWarnings("unchecked")
public class WordPressResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(WordPressResourceProvider.class);

    private final RestClient restClient;

    public WordPressResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "wordpress";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("posts", "pages", "categories", "tags", "users");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(
                new ResourceContextDescriptor("categories", 100, java.time.Duration.ofMinutes(10)),
                new ResourceContextDescriptor("tags", 100, java.time.Duration.ofMinutes(10)),
                new ResourceContextDescriptor("users", 100, java.time.Duration.ofMinutes(10))
        );
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String siteUrl = str(credentials.get("siteUrl")).replaceAll("/+$", "");
        String username = str(credentials.get("username"));
        String appPassword = str(credentials.get("applicationPassword"));
        if (siteUrl.isBlank() || username.isBlank() || appPassword.isBlank()) {
            throw new IllegalArgumentException("WordPress requires siteUrl, username, and applicationPassword.");
        }
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + appPassword).getBytes());
        String base = siteUrl + "/wp-json/wp/v2";

        return switch (resourceType) {
            case "posts"      -> listPosts(base, basicAuth);
            case "pages"      -> listPages(base, basicAuth);
            case "categories" -> listCategories(base, basicAuth);
            case "tags"       -> listTags(base, basicAuth);
            case "users"      -> listUsers(base, basicAuth);
            default -> List.of();
        };
    }

    private List<ResourceOption> listPosts(String base, String auth) {
        try {
            List<Map<String, Object>> posts = restClient.get()
                    .uri(base + "/posts?per_page=50&_fields=id,title,status,slug")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(List.class);
            if (posts == null) return List.of();
            return posts.stream().map(p -> {
                String id = String.valueOf(p.get("id"));
                Map<String, Object> titleObj = (Map<String, Object>) p.getOrDefault("title", Map.of());
                String title = str(titleObj.get("rendered"));
                String status = str(p.get("status"));
                return new ResourceOption(id, title.isBlank() ? "Post " + id : title, status);
            }).toList();
        } catch (Exception e) {
            logger.error("[wordpress] Failed to list posts: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listPages(String base, String auth) {
        try {
            List<Map<String, Object>> pages = restClient.get()
                    .uri(base + "/pages?per_page=50&_fields=id,title,status,slug")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(List.class);
            if (pages == null) return List.of();
            return pages.stream().map(p -> {
                String id = String.valueOf(p.get("id"));
                Map<String, Object> titleObj = (Map<String, Object>) p.getOrDefault("title", Map.of());
                String title = str(titleObj.get("rendered"));
                String slug = str(p.get("slug"));
                return new ResourceOption(id, title.isBlank() ? "Page " + id : title, "/" + slug);
            }).toList();
        } catch (Exception e) {
            logger.error("[wordpress] Failed to list pages: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listCategories(String base, String auth) {
        try {
            List<Map<String, Object>> cats = restClient.get()
                    .uri(base + "/categories?per_page=100&_fields=id,name,count")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(List.class);
            if (cats == null) return List.of();
            return cats.stream().map(c -> {
                String id = String.valueOf(c.get("id"));
                String name = str(c.get("name"));
                Object count = c.get("count");
                return new ResourceOption(id, name.isBlank() ? "Category " + id : name, count + " posts");
            }).toList();
        } catch (Exception e) {
            logger.error("[wordpress] Failed to list categories: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listTags(String base, String auth) {
        try {
            List<Map<String, Object>> tags = restClient.get()
                    .uri(base + "/tags?per_page=100&_fields=id,name,count")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(List.class);
            if (tags == null) return List.of();
            return tags.stream().map(t -> {
                String id = String.valueOf(t.get("id"));
                String name = str(t.get("name"));
                Object count = t.get("count");
                return new ResourceOption(id, name.isBlank() ? "Tag " + id : name, count + " posts");
            }).toList();
        } catch (Exception e) {
            logger.error("[wordpress] Failed to list tags: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listUsers(String base, String auth) {
        try {
            List<Map<String, Object>> users = restClient.get()
                    .uri(base + "/users?per_page=100&_fields=id,name,slug,roles")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(List.class);
            if (users == null) return List.of();
            return users.stream().map(u -> {
                String id = String.valueOf(u.get("id"));
                String name = str(u.get("name"));
                List<String> roles = (List<String>) u.getOrDefault("roles", List.of());
                String role = roles.isEmpty() ? "subscriber" : roles.get(0);
                return new ResourceOption(id, name.isBlank() ? "User " + id : name, role);
            }).toList();
        } catch (Exception e) {
            logger.error("[wordpress] Failed to list users: {}", e.getMessage());
            return List.of();
        }
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
