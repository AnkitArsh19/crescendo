package com.crescendo.apps.mattermost;

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
 * Fetches Mattermost resources: teams, channels, users.
 * Authenticates via Personal Access Token.
 */
@Component
@SuppressWarnings("unchecked")
public class MattermostResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(MattermostResourceProvider.class);

    private final RestClient restClient;

    public MattermostResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "mattermost";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("teams", "channels", "users");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("teams", 50, java.time.Duration.ofMinutes(5)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String baseUrl = str(credentials.get("baseUrl")).replaceAll("/+$", "");
        String token = str(credentials.get("accessToken"));
        if (baseUrl.isBlank() || token.isBlank()) {
            throw new IllegalArgumentException("Mattermost requires baseUrl and accessToken credentials.");
        }

        return switch (resourceType) {
            case "teams"    -> listTeams(baseUrl, token);
            case "channels" -> listChannels(baseUrl, token, params.getOrDefault("teamId", ""));
            case "users"    -> listUsers(baseUrl, token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listTeams(String baseUrl, String token) {
        try {
            List<Map<String, Object>> teams = restClient.get()
                    .uri(baseUrl + "/api/v4/teams?per_page=100")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(List.class);
            if (teams == null) return List.of();
            return teams.stream().map(t -> {
                String id = str(t.get("id"));
                String name = str(t.get("display_name"));
                String type = "open".equals(t.get("type")) ? "Public" : "Private";
                return new ResourceOption(id, name.isBlank() ? "Team " + id : name, type);
            }).toList();
        } catch (Exception e) {
            logger.error("[mattermost] Failed to list teams: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listChannels(String baseUrl, String token, String teamId) {
        try {
            String uri = teamId.isBlank()
                    ? baseUrl + "/api/v4/channels?per_page=100"
                    : baseUrl + "/api/v4/teams/" + teamId + "/channels?per_page=100";
            List<Map<String, Object>> channels = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(List.class);
            if (channels == null) return List.of();
            return channels.stream()
                    .filter(c -> !"D".equals(c.get("type"))) // exclude direct messages
                    .map(c -> {
                        String id = str(c.get("id"));
                        String display = str(c.get("display_name"));
                        String type = "O".equals(c.get("type")) ? "Public" : "Private";
                        return new ResourceOption(id, display.isBlank() ? "Channel " + id : display, type);
                    }).toList();
        } catch (Exception e) {
            logger.error("[mattermost] Failed to list channels: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listUsers(String baseUrl, String token) {
        try {
            List<Map<String, Object>> users = restClient.get()
                    .uri(baseUrl + "/api/v4/users?per_page=100")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(List.class);
            if (users == null) return List.of();
            return users.stream().map(u -> {
                String id = str(u.get("id"));
                String username = str(u.get("username"));
                String fn = str(u.get("first_name"));
                String ln = str(u.get("last_name"));
                String name = (fn + " " + ln).trim();
                if (name.isBlank()) name = username;
                return new ResourceOption(id, name.isBlank() ? "User " + id : name, "@" + username);
            }).toList();
        } catch (Exception e) {
            logger.error("[mattermost] Failed to list users: {}", e.getMessage());
            return List.of();
        }
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
