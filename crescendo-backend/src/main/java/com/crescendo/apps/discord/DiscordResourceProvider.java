package com.crescendo.apps.discord;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Discord resources (guilds/servers, text channels) with cascading:
 * guilds → channels (dependent on selected guild).
 * <p>
 * Supports both bot token ({@code Bot <token>}) and OAuth access token
 * ({@code Bearer <token>}) auth styles.
 */
@Component
public class DiscordResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(DiscordResourceProvider.class);
    private static final String DISCORD_API = "https://discord.com/api/v10";

    private final RestClient restClient;

    public DiscordResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "discord";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("guilds", "channels", "roles", "members");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String authHeader = buildAuthHeader(credentials);

        return switch (resourceType) {
            case "guilds" -> listGuilds(authHeader);
            case "channels" -> listChannels(authHeader, requireParam(params, "guildId"));
            case "roles" -> listRoles(authHeader, requireParam(params, "guildId"));
            case "members" -> listMembers(authHeader, requireParam(params, "guildId"));
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listGuilds(String authHeader) {
        try {
            // For bot tokens: GET /users/@me/guilds returns shared guilds
            List<Map<String, Object>> guilds = restClient.get()
                    .uri(DISCORD_API + "/users/@me/guilds")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(List.class);

            if (guilds == null) return List.of();

            return guilds.stream()
                    .map(g -> new ResourceOption(
                            String.valueOf(g.get("id")),
                            String.valueOf(g.get("name")),
                            "ID: " + g.get("id")))
                    .toList();

        } catch (Exception e) {
            logger.error("[discord] Failed to list guilds: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listChannels(String authHeader, String guildId) {
        try {
            List<Map<String, Object>> channels = restClient.get()
                    .uri(DISCORD_API + "/guilds/" + guildId + "/channels")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(List.class);

            if (channels == null) return List.of();

            // Filter to text channels only (type 0) and announcement channels (type 5)
            return channels.stream()
                    .filter(ch -> {
                        Object type = ch.get("type");
                        return type != null && (type.equals(0) || type.equals(5));
                    })
                    .map(ch -> {
                        String id = String.valueOf(ch.get("id"));
                        String name = String.valueOf(ch.get("name"));
                        Object type = ch.get("type");
                        String typeLabel = type.equals(5) ? "📢 Announcement" : "💬 Text";
                        return new ResourceOption(id, "#" + name, typeLabel);
                    })
                    .toList();

        } catch (Exception e) {
            logger.error("[discord] Failed to list channels for guild {}: {}", guildId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listRoles(String authHeader, String guildId) {
        try {
            List<Map<String, Object>> roles = restClient.get()
                    .uri(DISCORD_API + "/guilds/" + guildId + "/roles")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(List.class);

            if (roles == null) return List.of();

            return roles.stream()
                    // Filter out @everyone role and managed/integration roles
                    .filter(r -> !String.valueOf(r.get("name")).equals("@everyone")
                                 && !Boolean.TRUE.equals(r.get("managed")))
                    .map(r -> new ResourceOption(
                            String.valueOf(r.get("id")),
                            String.valueOf(r.get("name")),
                            "Position: " + r.get("position")))
                    .toList();

        } catch (Exception e) {
            logger.error("[discord] Failed to list roles for guild {}: {}", guildId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listMembers(String authHeader, String guildId) {
        try {
            List<Map<String, Object>> members = restClient.get()
                    .uri(DISCORD_API + "/guilds/" + guildId + "/members?limit=100")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(List.class);

            if (members == null) return List.of();

            return members.stream()
                    .filter(m -> m.get("user") != null)
                    .map(m -> {
                        Map<String, Object> user = (Map<String, Object>) m.get("user");
                        String id = String.valueOf(user.get("id"));
                        String username = String.valueOf(user.get("username"));
                        String nick = m.get("nick") != null ? String.valueOf(m.get("nick")) : null;
                        String displayName = nick != null ? nick + " (" + username + ")" : username;
                        return new ResourceOption(id, displayName, "ID: " + id);
                    })
                    .toList();

        } catch (Exception e) {
            logger.error("[discord] Failed to list members for guild {}: {}", guildId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Builds the Authorization header, handling both bot tokens and OAuth tokens.
     */
    private String buildAuthHeader(Map<String, Object> credentials) {
        logger.debug("[discord] Credential keys present: {}", credentials.keySet());

        // Bot token (API key auth)
        Object botToken = credentials.get("botToken");
        if (botToken != null && !botToken.toString().isBlank()) {
            String token = botToken.toString();
            logger.debug("[discord] Using Bot token (length={})", token.length());
            return "Bot " + token;
        }
        // OAuth access token
        Object accessToken = credentials.get("accessToken");
        if (accessToken != null && !accessToken.toString().isBlank()) {
            String token = accessToken.toString();
            logger.debug("[discord] Using Bearer token (length={})", token.length());
            return "Bearer " + token;
        }

        logger.error("[discord] No valid token found in credentials. Keys: {}", credentials.keySet());
        throw new IllegalArgumentException("Discord connection is missing both 'botToken' and 'accessToken'. "
                + "Please edit the connection and provide a valid bot token or reconnect with OAuth.");
    }

    private String requireParam(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value;
    }
}
