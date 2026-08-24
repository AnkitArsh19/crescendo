package com.crescendo.apps.jira;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JiraResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(JiraResourceProvider.class);

    private static final List<ResourceOption> STANDARD_ISSUE_TYPES = List.of(
            new ResourceOption("10001", "Task", "A small, distinct piece of work"),
            new ResourceOption("10002", "Bug", "A problem which impairs or prevents function"),
            new ResourceOption("10003", "Story", "Functionality or requirement to deliver"),
            new ResourceOption("10004", "Epic", "A large body of work containing multiple issues")
    );

    @Override
    public String appKey() {
        return "jira";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("projects", "issueTypes", "issues");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String domain = extract(credentials, "domain", "host", "siteUrl");
        String email = extract(credentials, "email", "user", "username");
        String apiToken = extract(credentials, "apiToken", "token", "apiKey", "password");

        if (domain == null || email == null || apiToken == null) {
            logger.warn("[jira] Incomplete credentials for resource listing");
            if (resourceType.equals("issueTypes")) return STANDARD_ISSUE_TYPES;
            return List.of();
        }

        return switch (resourceType) {
            case "projects" -> listProjects(domain, email, apiToken);
            case "issueTypes" -> listIssueTypes(domain, email, apiToken);
            case "issues" -> listIssues(domain, email, apiToken);
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

    private RestClient createClient(String domain, String email, String apiToken) {
        String normalizedDomain = domain.startsWith("http") ? domain : "https://" + domain;
        if (!normalizedDomain.contains(".atlassian.net") && !normalizedDomain.contains("://")) {
            normalizedDomain = "https://" + normalizedDomain + ".atlassian.net";
        }

        String auth = Base64.getEncoder().encodeToString((email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));
        return RestClient.builder()
                .baseUrl(normalizedDomain)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                .build();
    }

    private List<ResourceOption> listProjects(String domain, String email, String apiToken) {
        try {
            List<Map<String, Object>> projects = createClient(domain, email, apiToken).get()
                    .uri("/rest/api/3/project")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (projects == null) return List.of();
            List<ResourceOption> options = new ArrayList<>();
            for (Map<String, Object> p : projects) {
                String key = String.valueOf(p.get("key"));
                String name = String.valueOf(p.get("name"));
                options.add(new ResourceOption(key, name + " (" + key + ")"));
            }
            return options;
        } catch (Exception e) {
            logger.warn("[jira] Error fetching projects: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listIssueTypes(String domain, String email, String apiToken) {
        try {
            List<Map<String, Object>> types = createClient(domain, email, apiToken).get()
                    .uri("/rest/api/3/issuetype")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (types != null && !types.isEmpty()) {
                List<ResourceOption> options = new ArrayList<>();
                for (Map<String, Object> t : types) {
                    String id = String.valueOf(t.get("id"));
                    String name = String.valueOf(t.get("name"));
                    String desc = t.get("description") != null ? String.valueOf(t.get("description")) : null;
                    options.add(new ResourceOption(id, name, desc));
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[jira] Error fetching issue types, using standard: {}", e.getMessage());
        }
        return STANDARD_ISSUE_TYPES;
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listIssues(String domain, String email, String apiToken) {
        try {
            Map<String, Object> resp = createClient(domain, email, apiToken).get()
                    .uri("/rest/api/3/search?jql=order+by+created+DESC&maxResults=50")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("issues") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String key = String.valueOf(item.get("key"));
                        Map<?, ?> fields = (Map<?, ?>) item.get("fields");
                        String summary = fields != null && fields.get("summary") != null
                                ? String.valueOf(fields.get("summary"))
                                : key;
                        options.add(new ResourceOption(key, key + ": " + summary));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[jira] Error fetching issues: {}", e.getMessage());
        }
        return List.of();
    }
}
