package com.crescendo.apps.gitlab;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches GitLab resources: projects, branches, labels.
 * Uses GitLab REST API v4.
 */
@Component
public class GitLabResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GitLabResourceProvider.class);

    @Override
    public String appKey() {
        return "gitlab";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("projects", "branches", "labels");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
            String resourceType,
            Map<String, String> params) {
        String accessToken = credentials.get("accessToken").toString();
        String baseUrl = credentials.containsKey("baseUrl") ? credentials.get("baseUrl").toString() : "https://gitlab.com";
        String apiUrl = baseUrl.endsWith("/") ? baseUrl + "api/v4" : baseUrl + "/api/v4";

        return switch (resourceType) {
            case "projects" -> listProjects(accessToken, apiUrl);
            case "branches" -> listBranches(accessToken, apiUrl, params.get("projectId"));
            case "labels" -> listLabels(accessToken, apiUrl, params.get("projectId"));
            default -> List.of();
        };
    }

    private List<ResourceOption> listProjects(String accessToken, String apiUrl) {
        try {
            List<Map<String, Object>> projects = restClient(accessToken)
                    .get()
                    .uri(apiUrl + "/projects?membership=true&per_page=100&order_by=last_activity_at")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (projects == null)
                return List.of();

            return projects.stream()
                    .map(p -> new ResourceOption(
                            p.get("id").toString(),
                            p.get("path_with_namespace").toString(),
                            p.get("description") != null ? p.get("description").toString() : null))
                    .toList();
        } catch (Exception e) {
            logger.error("[gitlab] Failed to list projects: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listBranches(String accessToken, String apiUrl, String projectId) {
        if (projectId == null || projectId.isBlank())
            return List.of();
        try {
            List<Map<String, Object>> branches = restClient(accessToken)
                    .get()
                    .uri(apiUrl + "/projects/{projectId}/repository/branches?per_page=100", projectId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (branches == null)
                return List.of();

            return branches.stream()
                    .map(b -> new ResourceOption(
                            b.get("name").toString(),
                            b.get("name").toString(),
                            Boolean.TRUE.equals(b.get("default")) ? "default" : null))
                    .toList();
        } catch (Exception e) {
            logger.error("[gitlab] Failed to list branches: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listLabels(String accessToken, String apiUrl, String projectId) {
        if (projectId == null || projectId.isBlank())
            return List.of();
        try {
            List<Map<String, Object>> labels = restClient(accessToken)
                    .get()
                    .uri(apiUrl + "/projects/{projectId}/labels?per_page=100", projectId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (labels == null)
                return List.of();

            return labels.stream()
                    .map(l -> new ResourceOption(
                            l.get("id").toString(),
                            l.get("name").toString(),
                            l.get("color") != null ? l.get("color").toString() : null))
                    .toList();
        } catch (Exception e) {
            logger.error("[gitlab] Failed to list labels: {}", e.getMessage());
            return List.of();
        }
    }

    private RestClient restClient(String accessToken) {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
