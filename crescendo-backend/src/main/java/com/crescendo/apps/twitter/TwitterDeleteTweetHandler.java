package com.crescendo.apps.twitter;

import com.crescendo.execution.action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Deletes a tweet via X API v2 DELETE /2/tweets/{id}.
 */
@ActionMapping(appKey = "twitter", actionKey = "delete-tweet")
public class TwitterDeleteTweetHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(TwitterDeleteTweetHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null) return ActionResult.failure("X requires an OAuth2 accessToken");

        String tweetId = config.get("tweetId") != null ? config.get("tweetId").toString() : null;
        if (tweetId == null) return ActionResult.failure("'tweetId' is required");

        logger.info("[twitter] Deleting tweet '{}'", tweetId);
        try {
            Map<String, Object> resp = restClient.delete()
                    .uri("https://api.twitter.com/2/tweets/" + tweetId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "twitter");
            out.put("action", "delete-tweet");
            out.put("tweetId", tweetId);
            out.put("deleted", resp != null && resp.containsKey("data") ? ((Map<?,?>)resp.get("data")).get("deleted") : true);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("X delete-tweet failed: " + e.getMessage());
        }
    }
}
