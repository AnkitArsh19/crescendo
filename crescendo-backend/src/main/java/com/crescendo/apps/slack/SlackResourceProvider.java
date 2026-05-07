package com.crescendo.apps.slack;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Slack resources (channels, users) using the connected bot token.
 */
@Component
public class SlackResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(SlackResourceProvider.class);
    private static final String SLACK_API = "https://slack.com/api";

    private final RestClient restClient;

    public SlackResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "slack";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("channels", "users");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String token = extractToken(credentials);

        return switch (resourceType) {
            case "channels" -> listChannels(token);
            case "users" -> listUsers(token);
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listChannels(String token) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(SLACK_API + "/conversations.list?types=public_channel,private_channel&limit=200&exclude_archived=true")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
                logger.warn("[slack] conversations.list failed: {}", response);
                return List.of();
            }

            List<Map<String, Object>> channels = (List<Map<String, Object>>) response.get("channels");
            if (channels == null) return List.of();

            return channels.stream()
                    .map(ch -> {
                        String id = String.valueOf(ch.get("id"));
                        String name = String.valueOf(ch.get("name"));
                        boolean isPrivate = Boolean.TRUE.equals(ch.get("is_private"));
                        String desc = (isPrivate ? "🔒 Private" : "#") + " · " + id;
                        return new ResourceOption(id, "#" + name, desc);
                    })
                    .toList();

        } catch (Exception e) {
            logger.error("[slack] Failed to list channels: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listUsers(String token) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(SLACK_API + "/users.list?limit=200")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("ok"))) return List.of();

            List<Map<String, Object>> members = (List<Map<String, Object>>) response.get("members");
            if (members == null) return List.of();

            return members.stream()
                    .filter(m -> !Boolean.TRUE.equals(m.get("is_bot")) && !Boolean.TRUE.equals(m.get("deleted")))
                    .map(m -> {
                        String id = String.valueOf(m.get("id"));
                        String name = String.valueOf(m.get("real_name"));
                        if (name.isBlank()) name = String.valueOf(m.get("name"));
                        return new ResourceOption(id, name);
                    })
                    .toList();

        } catch (Exception e) {
            logger.error("[slack] Failed to list users: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractToken(Map<String, Object> credentials) {
        // Supports both OAuth accessToken and manual botToken
        Object token = credentials.get("accessToken");
        if (token == null) token = credentials.get("botToken");
        if (token == null || token.toString().isBlank()) {
            throw new IllegalArgumentException("Slack connection is missing an access token");
        }
        return token.toString();
    }
}
