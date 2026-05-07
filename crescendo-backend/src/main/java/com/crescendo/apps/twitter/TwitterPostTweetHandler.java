package com.crescendo.apps.twitter;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "twitter", actionKey = "post-tweet")
public class TwitterPostTweetHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TwitterPostTweetHandler.class);
    private static final String TWITTER_API = "https://api.twitter.com/2";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null || token.isBlank()) {
            return ActionResult.failure("X requires 'accessToken' in connection credentials");
        }

        String text = config.get("text") != null ? config.get("text").toString() : null;
        if (text == null || text.isBlank()) return ActionResult.failure("'text' is required");

        try {
            String response = RestClient.create()
                    .post()
                    .uri(TWITTER_API + "/tweets")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[twitter] Tweet posted successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[twitter] Post tweet failed", e);
            return ActionResult.failure("X post failed: " + e.getMessage());
        }
    }
}
