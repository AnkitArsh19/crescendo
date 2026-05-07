package com.crescendo.apps.spotify;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "spotify", actionKey = "get-playlist")
public class SpotifyGetPlaylistHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyGetPlaylistHandler.class);
    private static final String SPOTIFY_API = "https://api.spotify.com/v1";

    private final SpotifyClientCredentialsHelper credentialsHelper;

    public SpotifyGetPlaylistHandler(SpotifyClientCredentialsHelper credentialsHelper) {
        this.credentialsHelper = credentialsHelper;
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token;
        try {
            token = credentialsHelper.resolveAccessToken(creds);
        } catch (SpotifyClientCredentialsHelper.SpotifyAuthException e) {
            return ActionResult.failure(e.getMessage());
        }

        String playlistInput = config.get("playlistId") != null ? config.get("playlistId").toString().trim() : null;
        if (playlistInput == null || playlistInput.isBlank()) return ActionResult.failure("'playlistId' is required");

        // Extract ID from URL if a full Spotify URL was provided
        String playlistId = extractPlaylistId(playlistInput);

        try {
            String response = RestClient.create()
                    .get()
                    .uri(SPOTIFY_API + "/playlists/" + playlistId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[spotify] Playlist fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[spotify] Get playlist failed", e);
            return ActionResult.failure("Spotify get playlist failed: " + e.getMessage());
        }
    }

    /**
     * Extracts the playlist ID from a Spotify URL or returns the input as-is if it's already an ID.
     * Handles URLs like:
     * - https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M
     * - https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc123
     * - spotify:playlist:37i9dQZF1DXcBWIGoYBM5M
     */
    private String extractPlaylistId(String input) {
        if (input == null) return null;
        input = input.trim();
        // Handle URL: https://open.spotify.com/playlist/{id}?...
        if (input.contains("open.spotify.com/playlist/")) {
            String afterPlaylist = input.substring(input.indexOf("/playlist/") + "/playlist/".length());
            // Remove query params
            int qIndex = afterPlaylist.indexOf('?');
            return qIndex > 0 ? afterPlaylist.substring(0, qIndex) : afterPlaylist;
        }
        // Handle URI: spotify:playlist:{id}
        if (input.startsWith("spotify:playlist:")) {
            return input.substring("spotify:playlist:".length());
        }
        // Already an ID
        return input;
    }
}

