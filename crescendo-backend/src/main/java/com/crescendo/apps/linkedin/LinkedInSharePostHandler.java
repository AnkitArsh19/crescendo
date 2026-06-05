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

/**
 * Creates a text post on LinkedIn using the Posts API.
 *
 * <p>Uses the current {@code /rest/posts} endpoint — the legacy {@code /v2/ugcPosts}
 * endpoint was deprecated by LinkedIn and replaced by the Community Management API's
 * Posts API.
 *
 * <p>Connection credentials: {@code accessToken}, {@code personUrn}
 * <p>Config: {@code text}, {@code visibility}
 *
 * @see <a href="https://learn.microsoft.com/en-us/linkedin/marketing/community-management/shares/posts-api">LinkedIn Posts API</a>
 */
@ActionMapping(appKey = "linkedin", actionKey = "share-post")
public class LinkedInSharePostHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LinkedInSharePostHandler.class);

    /**
     * New Posts API endpoint — replaces the deprecated /v2/ugcPosts.
     */
    private static final String POSTS_API = "https://api.linkedin.com/rest/posts";

    /**
     * LinkedIn API version in YYYYMM format.
     * Required header for the versioned /rest/ endpoints.
     */
    private static final String LINKEDIN_VERSION = "202401";

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
            // Posts API payload format (replaces ugcPosts format)
            Map<String, Object> body = new HashMap<>();
            body.put("author", personUrn);
            body.put("commentary", text);
            body.put("visibility", visibility);
            body.put("distribution", Map.of(
                    "feedDistribution", "MAIN_FEED"
            ));
            body.put("lifecycleState", "PUBLISHED");

            String response = RestClient.create()
                    .post()
                    .uri(POSTS_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("LinkedIn-Version", LINKEDIN_VERSION)
                    .header("X-Restli-Protocol-Version", "2.0.0")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[linkedin] Post shared successfully via Posts API");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[linkedin] Share post failed", e);
            return ActionResult.failure("LinkedIn share post failed: " + e.getMessage());
        }
    }
}
