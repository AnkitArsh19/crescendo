package com.crescendo.apps.spotify;

import com.crescendo.execution.action.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Saves a track to the user's Spotify library via PUT /v1/me/tracks.
 */
@ActionMapping(appKey = "spotify", actionKey = "save-track")
public class SpotifySaveTrackHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null)
            return ActionResult.failure("Spotify requires an OAuth2 accessToken");

        String trackId = config.get("trackId") != null ? config.get("trackId").toString() : null;
        if (trackId == null)
            return ActionResult.failure("'trackId' is required");

        // Strip URI prefix if provided
        if (trackId.startsWith("spotify:track:"))
            trackId = trackId.substring(14);
        if (trackId.contains("/"))
            trackId = trackId.substring(trackId.lastIndexOf("/") + 1);

        try {
            restClient.put()
                    .uri("https://api.spotify.com/v1/me/tracks")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("ids", List.of(trackId)))
                    .retrieve().toBodilessEntity();

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "spotify");
            out.put("action", "save-track");
            out.put("trackId", trackId);
            out.put("saved", true);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Spotify save-track failed: " + e.getMessage());
        }
    }
}
