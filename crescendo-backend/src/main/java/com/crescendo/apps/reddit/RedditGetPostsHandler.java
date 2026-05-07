package com.crescendo.apps.reddit;

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

@ActionMapping(appKey = "reddit", actionKey = "get-posts")
public class RedditGetPostsHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RedditGetPostsHandler.class);
    private static final String REDDIT_API = "https://oauth.reddit.com";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null || token.isBlank()) {
            return ActionResult.failure("Reddit requires 'accessToken' in connection credentials");
        }

        String subreddit = config.get("subreddit") != null ? config.get("subreddit").toString() : null;
        if (subreddit == null || subreddit.isBlank()) return ActionResult.failure("'subreddit' is required");

        String sort = config.getOrDefault("sort", "hot").toString();
        int limit = 25;
        if (config.containsKey("limit")) {
            try { limit = Integer.parseInt(config.get("limit").toString()); }
            catch (NumberFormatException ignored) {}
        }

        try {
            String response = RestClient.create()
                    .get()
                    .uri(REDDIT_API + "/r/" + subreddit + "/" + sort + "?limit=" + limit)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("User-Agent", "Crescendo/1.0")
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            output.put("subreddit", subreddit);
            logger.info("[reddit] Posts fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[reddit] Get posts failed for r/{}", subreddit, e);
            return ActionResult.failure("Reddit get posts failed: " + e.getMessage());
        }
    }
}
