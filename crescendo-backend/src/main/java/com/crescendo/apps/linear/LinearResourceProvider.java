package com.crescendo.apps.linear;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceContextDescriptor;
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
 * Fetches Linear resources using the GraphQL API.
 * Supports: teams, projects, states (workflow states within a team)
 */
@Component
public class LinearResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(LinearResourceProvider.class);
    private static final String LINEAR_API = "https://api.linear.app/graphql";

    @Override
    public String appKey() {
        return "linear";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("teams", "projects", "states");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(
                new ResourceContextDescriptor("teams", 50, java.time.Duration.ofMinutes(5)),
                new ResourceContextDescriptor("projects", 50, java.time.Duration.ofMinutes(5)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
            String resourceType,
            Map<String, String> params) {
        String token = extractToken(credentials);

        return switch (resourceType) {
            case "teams" -> listTeams(token);
            case "projects" -> listProjects(token);
            case "states" -> listStates(token, params.get("teamId"));
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listTeams(String accessToken) {
        try {
            String query = """
                    { "query": "{ teams { nodes { id name key } } }" }
                    """;
            Map<String, Object> response = graphQL(accessToken, query);
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Map<String, Object> teams = (Map<String, Object>) data.get("teams");
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) teams.get("nodes");

            return nodes.stream()
                    .map(t -> new ResourceOption(
                            t.get("id").toString(),
                            t.get("name").toString(),
                            t.get("key") != null ? t.get("key").toString() : null))
                    .toList();
        } catch (Exception e) {
            logger.error("[linear] Failed to list teams: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listProjects(String accessToken) {
        try {
            String query = """
                    { "query": "{ projects(first: 100) { nodes { id name state } } }" }
                    """;
            Map<String, Object> response = graphQL(accessToken, query);
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Map<String, Object> projects = (Map<String, Object>) data.get("projects");
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) projects.get("nodes");

            return nodes.stream()
                    .map(p -> new ResourceOption(
                            p.get("id").toString(),
                            p.get("name").toString(),
                            p.get("state") != null ? p.get("state").toString() : null))
                    .toList();
        } catch (Exception e) {
            logger.error("[linear] Failed to list projects: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listStates(String accessToken, String teamId) {
        if (teamId == null || teamId.isBlank())
            return List.of();
        try {
            String query = String.format("""
                    { "query": "{ team(id: \\"%s\\") { states { nodes { id name type } } } }" }
                    """, teamId);
            Map<String, Object> response = graphQL(accessToken, query);
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Map<String, Object> team = (Map<String, Object>) data.get("team");
            Map<String, Object> states = (Map<String, Object>) team.get("states");
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) states.get("nodes");

            return nodes.stream()
                    .map(s -> new ResourceOption(
                            s.get("id").toString(),
                            s.get("name").toString(),
                            s.get("type") != null ? s.get("type").toString() : null))
                    .toList();
        } catch (Exception e) {
            logger.error("[linear] Failed to list states for team {}: {}", teamId, e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> graphQL(String token, String body) {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, token)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .post()
                .uri(LINEAR_API)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private String extractToken(Map<String, Object> credentials) {
        if (credentials == null) return null;
        Object tokenObj = credentials.get("accessToken");
        if (tokenObj != null && !tokenObj.toString().isBlank()) return "Bearer " + tokenObj.toString();
        Object apiKey = credentials.get("apiKey");
        return apiKey != null && !apiKey.toString().isBlank() ? apiKey.toString() : null;
    }
}
