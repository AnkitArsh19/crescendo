package com.crescendo.apps.spotify;

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
 * Fetches Spotify resources: playlists, tracks in a playlist.
 * Uses the Spotify Web API v1.
 *
 * <p>Supports both OAuth tokens (admin users) and Client Credentials
 * tokens (normal users who provide their own clientId/clientSecret).
 */
@Component
public class SpotifyResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyResourceProvider.class);
    private static final String SPOTIFY_API = "https://api.spotify.com/v1";

    public SpotifyResourceProvider() {
    }

    @Override
    public String appKey() {
        return "spotify";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("playlists", "tracks");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = credentials != null && credentials.get("accessToken") != null ? credentials.get("accessToken").toString() : null;
        if (accessToken == null || accessToken.isBlank()) {
            logger.warn("[spotify] Cannot resolve token for resource listing");
            return List.of();
        }

        return switch (resourceType) {
            case "playlists" -> listPlaylists(accessToken);
            case "tracks" -> listTracks(accessToken, params.get("playlistId"));
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listPlaylists(String accessToken) {
        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(SPOTIFY_API + "/me/playlists?limit=50")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null) return List.of();

            return items.stream()
                    .map(p -> new ResourceOption(
                            p.get("id").toString(),
                            p.get("name").toString(),
                            p.get("tracks") instanceof Map<?, ?> tracks
                                    ? tracks.get("total") + " tracks"
                                    : null
                    ))
                    .toList();
        } catch (Exception e) {
            logger.error("[spotify] Failed to list playlists: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listTracks(String accessToken, String playlistId) {
        if (playlistId == null || playlistId.isBlank()) return List.of();
        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(SPOTIFY_API + "/playlists/{id}/tracks?limit=50&fields=items(track(id,name,artists))", playlistId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null) return List.of();

            return items.stream()
                    .filter(item -> item.get("track") instanceof Map<?, ?>)
                    .map(item -> {
                        Map<String, Object> track = (Map<String, Object>) item.get("track");
                        String artistName = null;
                        if (track.get("artists") instanceof List<?> artists && !artists.isEmpty()) {
                            Object first = artists.get(0);
                            if (first instanceof Map<?, ?> artist) {
                                artistName = artist.get("name") != null ? artist.get("name").toString() : null;
                            }
                        }
                        return new ResourceOption(
                                "spotify:track:" + track.get("id"),
                                track.get("name").toString(),
                                artistName
                        );
                    })
                    .toList();
        } catch (Exception e) {
            logger.error("[spotify] Failed to list tracks for playlist {}: {}", playlistId, e.getMessage());
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
