package com.crescendo.apps.spotify;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SpotifyPlaylistHandlers {

    private static final Pattern SPOTIFY_PLAYLIST_URL_PATTERN = Pattern.compile("spotify\\.com/playlist/([a-zA-Z0-9]+)");
    private static final Pattern SPOTIFY_TRACK_URL_PATTERN = Pattern.compile("spotify\\.com/track/([a-zA-Z0-9]+)");

    private final SpotifySupport spotifySupport;

    public SpotifyPlaylistHandlers(SpotifySupport spotifySupport) {
        this.spotifySupport = spotifySupport;
    }

    private String getAuth(ActionContext context) {
        String token = spotifySupport.resolveAccessToken(context.credentials());
        return "Bearer " + token;
    }

    @ActionMapping(appKey = "spotify", actionKey = "create-playlist")
    public Object createPlaylist(ActionContext context) throws Exception {
        String name = context.getString("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Playlist name is required");
        }

        String description = context.getString("description");
        boolean isPublic = "true".equalsIgnoreCase(context.getString("isPublic"));

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        if (description != null && !description.isBlank()) {
            body.put("description", description);
        }
        body.put("public", isPublic);

        return RestClient.builder()
                .url("https://api.spotify.com/v1/me/playlists")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "spotify", actionKey = "get-playlist")
    public Object getPlaylist(ActionContext context) throws Exception {
        String input = context.getString("playlistId");
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Playlist URL, URI, or ID is required");
        }

        String playlistId = resolvePlaylistId(input);
        return RestClient.builder()
                .url("https://api.spotify.com/v1/playlists/" + playlistId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "spotify", actionKey = "add-to-playlist")
    public Object addToPlaylist(ActionContext context) throws Exception {
        String playlistInput = context.getString("playlistId");
        String trackInput = context.getString("trackUri");

        if (playlistInput == null || playlistInput.isBlank()) {
            throw new IllegalArgumentException("Target playlist is required");
        }
        if (trackInput == null || trackInput.isBlank()) {
            throw new IllegalArgumentException("Track URI, URL, or ID is required");
        }

        String playlistId = resolvePlaylistId(playlistInput);
        String trackUri = resolveTrackUri(trackInput);

        return RestClient.builder()
                .url("https://api.spotify.com/v1/playlists/" + playlistId + "/tracks")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("uris", List.of(trackUri)))
                .execute();
    }

    public static String resolvePlaylistId(String input) {
        String trimmed = input.trim();
        Matcher matcher = SPOTIFY_PLAYLIST_URL_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (trimmed.startsWith("spotify:playlist:")) {
            return trimmed.substring("spotify:playlist:".length()).trim();
        }
        return trimmed;
    }

    public static String resolveTrackUri(String input) {
        String trimmed = input.trim();
        if (trimmed.startsWith("spotify:track:")) {
            return trimmed;
        }
        Matcher matcher = SPOTIFY_TRACK_URL_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return "spotify:track:" + matcher.group(1);
        }
        return "spotify:track:" + trimmed;
    }
}

