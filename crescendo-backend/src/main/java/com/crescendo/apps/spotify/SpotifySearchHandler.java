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

@ActionMapping(appKey = "spotify", actionKey = "search")
public class SpotifySearchHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpotifySearchHandler.class);
    private static final String SPOTIFY_API = "https://api.spotify.com/v1";

    private final SpotifyClientCredentialsHelper credentialsHelper;

    public SpotifySearchHandler(SpotifyClientCredentialsHelper credentialsHelper) {
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

        String query = config.get("query") != null ? config.get("query").toString() : null;
        if (query == null || query.isBlank()) return ActionResult.failure("'query' is required");

        String type = config.getOrDefault("type", "track").toString();
        int limit = 10;
        if (config.containsKey("limit")) {
            try { limit = Integer.parseInt(config.get("limit").toString()); }
            catch (NumberFormatException ignored) {}
        }

        try {
            String response = RestClient.create()
                    .get()
                    .uri(SPOTIFY_API + "/search?q={q}&type={type}&limit={limit}", query, type, limit)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[spotify] Search completed successfully, query={}", query);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[spotify] Search failed", e);
            return ActionResult.failure("Spotify search failed: " + e.getMessage());
        }
    }
}
