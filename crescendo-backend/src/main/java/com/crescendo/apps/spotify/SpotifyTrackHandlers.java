package com.crescendo.apps.spotify;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SpotifyTrackHandlers {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyTrackHandlers.class);
    private static final Pattern SPOTIFY_TRACK_URL_PATTERN = Pattern.compile("spotify\\.com/track/([a-zA-Z0-9]+)");
    private static final Pattern SPOTIFY_RAW_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{22}$");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SpotifySupport spotifySupport;

    public SpotifyTrackHandlers(SpotifySupport spotifySupport) {
        this.spotifySupport = spotifySupport;
    }

    private String getAuth(ActionContext context) {
        String token = spotifySupport.resolveAccessToken(context.credentials());
        return "Bearer " + token;
    }

    @ActionMapping(appKey = "spotify", actionKey = "save-track")
    public Object saveTrack(ActionContext context) throws Exception {
        String input = context.getString("trackId");
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Track ID, URL, URI, or song name is required");
        }

        // Check if connection is Client Credentials (API Key) vs User OAuth
        Map<String, Object> creds = context.credentials();
        boolean isApiKeyOnly = creds != null && creds.containsKey("clientId") && !creds.containsKey("accessToken");
        if (isApiKeyOnly) {
            throw new IllegalArgumentException(
                    "This Spotify connection was created using 'API Key' (Client Credentials), which does not have a user library. " +
                    "To save songs to your personal Liked Songs, please delete this connection in Connections and reconnect using 'Connect with OAuth' (logging into your Spotify user account).");
        }

        String authHeader = getAuth(context);
        String resolvedTrackId = resolveTrackId(input, authHeader);

        try {
            // Primary (Current Spotify Web API): PUT /v1/me/library?uris=spotify:track:{id}
            String trackUri = resolvedTrackId.startsWith("spotify:track:") ? resolvedTrackId : ("spotify:track:" + resolvedTrackId);
            try {
                RestClient.builder()
                        .url("https://api.spotify.com/v1/me/library?uris=" + trackUri)
                        .header("Authorization", authHeader)
                        .put()
                        .execute();
            } catch (Exception libErr) {
                // Fallback 1: Legacy PUT /v1/me/tracks?ids={id}
                try {
                    RestClient.builder()
                            .url("https://api.spotify.com/v1/me/tracks?ids=" + resolvedTrackId)
                            .header("Authorization", authHeader)
                            .put()
                            .execute();
                } catch (Exception queryErr) {
                    // Fallback 2: Legacy PUT /v1/me/tracks with JSON body {"ids": [...]}
                    RestClient.builder()
                            .url("https://api.spotify.com/v1/me/tracks")
                            .header("Authorization", authHeader)
                            .header("Content-Type", "application/json")
                            .put(Map.of("ids", List.of(resolvedTrackId)))
                            .execute();
                }
            }

            return Map.of(
                    "status", "success",
                    "message", "Track saved to Liked Songs (Your Library)",
                    "trackId", resolvedTrackId,
                    "uri", "spotify:track:" + resolvedTrackId,
                    "spotifyUrl", "https://open.spotify.com/track/" + resolvedTrackId
            );
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            if (errorMsg.contains("403")) {
                throw new RuntimeException(
                        "Spotify returned 403 Forbidden on /v1/me/tracks. " +
                        "In Spotify's Developer Dashboard, apps in 'Development Mode' require adding your Spotify account email under " +
                        "'User Management' (Users and Access) before write actions (like saving tracks) are allowed. " +
                        "Please verify your account email is listed under 'User Management' in developer.spotify.com/dashboard.");
            }
            throw e;
        }
    }

    @ActionMapping(appKey = "spotify", actionKey = "search")
    public Object search(ActionContext context) throws Exception {
        String query = context.getString("query");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query is required");
        }

        String type = (String) context.configuration().getOrDefault("type", "track");
        int limit = 10;
        if (context.configuration().get("limit") != null) {
            try {
                limit = Math.min(50, Math.max(1, Integer.parseInt(context.configuration().get("limit").toString())));
            } catch (NumberFormatException ignored) {}
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return RestClient.builder()
                .url("https://api.spotify.com/v1/search?q=" + encodedQuery + "&type=" + type + "&limit=" + limit)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    /**
     * Resolves a track ID from a Spotify URL, Spotify URI, raw 22-char ID, or searches for it by name.
     */
    public String resolveTrackId(String input, String authHeader) {
        String trimmed = input.trim();

        // 1. Spotify URL: https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT?si=...
        Matcher urlMatcher = SPOTIFY_TRACK_URL_PATTERN.matcher(trimmed);
        if (urlMatcher.find()) {
            return urlMatcher.group(1);
        }

        // 2. Spotify URI: spotify:track:4cOdK2wGLETKBW3PvgPWqT
        if (trimmed.startsWith("spotify:track:")) {
            return trimmed.substring("spotify:track:".length()).trim();
        }

        // 3. Raw 22-char Spotify ID: 4cOdK2wGLETKBW3PvgPWqT
        if (SPOTIFY_RAW_ID_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }

        // 4. Song name / artist query -> Search Spotify to get the best track match
        logger.info("[spotify] Searching Spotify for track: {}", trimmed);
        try {
            String encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
            Object searchResponse = RestClient.builder()
                    .url("https://api.spotify.com/v1/search?q=" + encoded + "&type=track&limit=1")
                    .header("Authorization", authHeader)
                    .get()
                    .execute();

            if (searchResponse instanceof Map<?, ?> map && map.containsKey("tracks")) {
                Object tracksObj = map.get("tracks");
                if (tracksObj instanceof Map<?, ?> tracksMap && tracksMap.containsKey("items")) {
                    Object itemsObj = tracksMap.get("items");
                    if (itemsObj instanceof List<?> itemsList && !itemsList.isEmpty()) {
                        Object firstItem = itemsList.get(0);
                        if (firstItem instanceof Map<?, ?> trackMap && trackMap.containsKey("id")) {
                            String foundId = trackMap.get("id").toString();
                            logger.info("[spotify] Found track '{}' -> {}", trackMap.get("name"), foundId);
                            return foundId;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("[spotify] Track search for '{}' failed: {}", trimmed, e.getMessage());
        }

        // Fallback: return sanitized input
        return trimmed.replaceAll("[^a-zA-Z0-9]", "");
    }
}

