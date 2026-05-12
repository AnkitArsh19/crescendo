package com.crescendo.apps.spotify;

import com.crescendo.execution.action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Creates a new Spotify playlist via POST /v1/users/{user_id}/playlists.
 */
@ActionMapping(appKey = "spotify", actionKey = "create-playlist")
public class SpotifyCreatePlaylistHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyCreatePlaylistHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null) return ActionResult.failure("Spotify requires an OAuth2 accessToken");

        String name = config.get("name") != null ? config.get("name").toString() : null;
        if (name == null) return ActionResult.failure("'name' is required");
        String description = config.get("description") != null ? config.get("description").toString() : "";
        boolean isPublic = !"false".equalsIgnoreCase(config.getOrDefault("isPublic", "true").toString());

        try {
            // Get current user ID
            Map<String, Object> me = restClient.get()
                    .uri("https://api.spotify.com/v1/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            String userId = me != null ? (String) me.get("id") : null;
            if (userId == null) return ActionResult.failure("Could not get Spotify user ID");

            Map<String, Object> body = Map.of("name", name, "description", description, "public", isPublic);
            Map<String, Object> resp = restClient.post()
                    .uri("https://api.spotify.com/v1/users/" + userId + "/playlists")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "spotify");
            out.put("action", "create-playlist");
            out.put("playlistId", resp != null ? resp.get("id") : null);
            out.put("externalUrl", resp != null ? ((Map<?,?>)resp.get("external_urls")).get("spotify") : null);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Spotify create-playlist failed: " + e.getMessage());
        }
    }
}
