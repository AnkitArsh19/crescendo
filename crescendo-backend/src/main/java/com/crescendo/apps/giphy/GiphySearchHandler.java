package com.crescendo.apps.giphy;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "giphy", actionKey = "search-gifs")
public class GiphySearchHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GiphySearchHandler.class);
    private static final String GIPHY_API = "https://api.giphy.com/v1/gifs";

    @Value("${crescendo.platform.giphy-api-key:}")
    private String platformApiKey;

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        // Use platform key first, fall back to user-provided credentials
        String apiKey = (platformApiKey != null && !platformApiKey.isBlank())
                ? platformApiKey
                : (creds != null ? (String) creds.get("apiKey") : null);
        if (apiKey == null || apiKey.isBlank()) return ActionResult.failure("Giphy requires an API key (configure in application.properties)");

        String query = config.get("query") != null ? config.get("query").toString() : null;
        if (query == null || query.isBlank()) return ActionResult.failure("'query' is required");

        int limit = 10;
        if (config.containsKey("limit")) {
            try { limit = Math.min(50, Integer.parseInt(config.get("limit").toString())); }
            catch (NumberFormatException ignored) {}
        }
        String rating = config.getOrDefault("rating", "g").toString();

        try {
            String response = RestClient.create()
                    .get()
                    .uri(GIPHY_API + "/search?api_key={key}&q={q}&limit={limit}&rating={rating}",
                            apiKey, query, limit, rating)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[giphy] Search completed, query={}", query);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[giphy] Search failed", e);
            return ActionResult.failure("Giphy search failed: " + e.getMessage());
        }
    }
}
