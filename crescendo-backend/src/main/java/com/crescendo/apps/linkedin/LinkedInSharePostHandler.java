package com.crescendo.apps.linkedin;

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

@ActionMapping(appKey = "linkedin", actionKey = "share-post")
public class LinkedInSharePostHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LinkedInSharePostHandler.class);
    private static final String LINKEDIN_API = "https://api.linkedin.com/v2";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = creds != null ? (String) creds.get("accessToken") : null;
        String personUrn = creds != null ? (String) creds.get("personUrn") : null;

        if (token == null || token.isBlank()) return ActionResult.failure("LinkedIn requires 'accessToken'");
        if (personUrn == null || personUrn.isBlank()) return ActionResult.failure("LinkedIn requires 'personUrn'");

        String text = config.get("text") != null ? config.get("text").toString() : null;
        if (text == null || text.isBlank()) return ActionResult.failure("'text' is required");

        String visibility = config.getOrDefault("visibility", "PUBLIC").toString();

        try {
            Map<String, Object> body = Map.of(
                    "author", personUrn,
                    "lifecycleState", "PUBLISHED",
                    "specificContent", Map.of(
                            "com.linkedin.ugc.ShareContent", Map.of(
                                    "shareCommentary", Map.of("text", text),
                                    "shareMediaCategory", "NONE"
                            )
                    ),
                    "visibility", Map.of(
                            "com.linkedin.ugc.MemberNetworkVisibility", visibility
                    )
            );

            String response = RestClient.create()
                    .post()
                    .uri(LINKEDIN_API + "/ugcPosts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("X-Restli-Protocol-Version", "2.0.0")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[linkedin] Post shared successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[linkedin] Share post failed", e);
            return ActionResult.failure("LinkedIn share post failed: " + e.getMessage());
        }
    }
}
