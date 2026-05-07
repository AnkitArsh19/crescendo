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

@ActionMapping(appKey = "giphy", actionKey = "random-gif")
public class GiphyRandomHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GiphyRandomHandler.class);
    private static final String GIPHY_API = "https://api.giphy.com/v1/gifs";

    @Value("${crescendo.platform.giphy-api-key:}")
    private String platformApiKey;

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = (platformApiKey != null && !platformApiKey.isBlank())
                ? platformApiKey
                : (creds != null ? (String) creds.get("apiKey") : null);
        if (apiKey == null || apiKey.isBlank()) return ActionResult.failure("Giphy requires an API key");

        String tag = config.get("tag") != null ? config.get("tag").toString() : "";
        String rating = config.getOrDefault("rating", "g").toString();

        try {
            String uri = GIPHY_API + "/random?api_key=" + apiKey + "&rating=" + rating;
            if (!tag.isBlank()) uri += "&tag=" + tag;

            String response = RestClient.create()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[giphy] Random GIF fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[giphy] Random failed", e);
            return ActionResult.failure("Giphy random failed: " + e.getMessage());
        }
    }
}
