package com.crescendo.apps.jokeapi;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "jokeapi", actionKey = "get-joke")
public class JokeApiGetJokeHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(JokeApiGetJokeHandler.class);
    private static final String JOKEAPI_URL = "https://v2.jokeapi.dev/joke";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();

        String category = config.getOrDefault("category", "Any").toString();
        String type = config.get("type") != null ? config.get("type").toString() : null;
        boolean safe = config.get("safe") == null || Boolean.parseBoolean(config.get("safe").toString());

        StringBuilder uri = new StringBuilder(JOKEAPI_URL + "/" + category + "?");
        if (type != null && !type.isBlank()) {
            uri.append("type=").append(type).append("&");
        }
        if (safe) {
            uri.append("safe-mode");
        }

        try {
            String response = RestClient.create()
                    .get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[jokeapi] Joke fetched, category={}", category);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[jokeapi] Fetch joke failed", e);
            return ActionResult.failure("Failed to fetch joke: " + e.getMessage());
        }
    }
}
