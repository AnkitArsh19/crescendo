package com.crescendo.apps.microsoftteams;

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
 * Fetches Microsoft Teams resources (teams, channels) via the Microsoft Graph
 * API.
 * Cascade: teams → channels.
 */
@Component
@SuppressWarnings("unchecked")
public class MicrosoftTeamsResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftTeamsResourceProvider.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0";

    private final RestClient restClient;

    public MicrosoftTeamsResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "microsoft-teams";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("teams", "channels", "members");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("teams", 50, java.time.Duration.ofMinutes(5)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
            String resourceType,
            Map<String, String> params) {
        String token = extractToken(credentials);

        return switch (resourceType) {
            case "teams" -> listTeams(token);
            case "channels" -> listChannels(token, requireParam(params, "teamId"));
            case "members" -> listMembers(token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listTeams(String token) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(GRAPH_API + "/me/joinedTeams")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("value"))
                return List.of();

            List<Map<String, Object>> teams = (List<Map<String, Object>>) response.get("value");
            return teams.stream()
                    .map(t -> new ResourceOption(
                            String.valueOf(t.get("id")),
                            String.valueOf(t.get("displayName")),
                            t.get("description") != null ? String.valueOf(t.get("description")) : null))
                    .toList();

        } catch (Exception e) {
            logger.error("[ms-teams] Failed to list teams: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listChannels(String token, String teamId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(GRAPH_API + "/teams/" + teamId + "/channels")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("value"))
                return List.of();

            List<Map<String, Object>> channels = (List<Map<String, Object>>) response.get("value");
            return channels.stream()
                    .map(ch -> new ResourceOption(
                            String.valueOf(ch.get("id")),
                            String.valueOf(ch.get("displayName")),
                            ch.get("description") != null ? String.valueOf(ch.get("description")) : null))
                    .toList();

        } catch (Exception e) {
            logger.error("[ms-teams] Failed to list channels for team {}: {}", teamId, e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listMembers(String token) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(GRAPH_API + "/users?$select=id,displayName,mail&$top=100")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("value"))
                return List.of();

            List<Map<String, Object>> users = (List<Map<String, Object>>) response.get("value");
            return users.stream()
                    .map(u -> new ResourceOption(
                            String.valueOf(u.get("id")),
                            String.valueOf(u.get("displayName")),
                            u.get("mail") != null ? String.valueOf(u.get("mail")) : null))
                    .toList();

        } catch (Exception e) {
            logger.error("[ms-teams] Failed to list members: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractToken(Map<String, Object> credentials) {
        Object token = credentials.get("accessToken");
        if (token == null || token.toString().isBlank()) {
            throw new IllegalArgumentException("Microsoft Teams connection is missing 'accessToken'");
        }
        return token.toString();
    }

    private String requireParam(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value;
    }
}
