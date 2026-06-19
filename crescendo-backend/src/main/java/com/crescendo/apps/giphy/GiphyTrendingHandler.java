package com.crescendo.apps.giphy;

import com.crescendo.execution.action.*;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Gets trending GIFs from Giphy via /v1/gifs/trending.
 */
@ActionMapping(appKey = "giphy", actionKey = "trending-gifs")
public class GiphyTrendingHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        // Giphy uses a public beta key by default or user-provided API key
        String apiKey = creds != null && creds.get("apiKey") != null
                ? creds.get("apiKey").toString()
                : "dc6zaTOxFJmzC";

        String limit = config.getOrDefault("limit", "10").toString();
        String rating = config.getOrDefault("rating", "g").toString();

        try {
            String url = "https://api.giphy.com/v1/gifs/trending?api_key=" + apiKey
                    + "&limit=" + limit + "&rating=" + rating;
            Map<String, Object> resp = restClient.get().uri(url).retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "giphy");
            out.put("action", "trending-gifs");
            if (resp != null && resp.containsKey("data")) {
                var data = (List<Map<String, Object>>) resp.get("data");
                out.put("resultCount", data.size());
                List<Map<String, String>> gifs = new ArrayList<>();
                for (var gif : data) {
                    var images = (Map<String, Object>) gif.get("images");
                    var original = (Map<String, Object>) images.get("original");
                    gifs.add(Map.of("id", gif.get("id").toString(), "title", gif.get("title").toString(),
                            "url", original.get("url").toString()));
                }
                out.put("gifs", gifs);
            }
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Giphy trending failed: " + e.getMessage());
        }
    }
}
