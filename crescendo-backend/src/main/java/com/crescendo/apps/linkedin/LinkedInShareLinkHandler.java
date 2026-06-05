package com.crescendo.apps.linkedin;

import com.crescendo.execution.action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Shares a post with a link attachment on LinkedIn via the Posts API.
 *
 * <p>Uses the current {@code /rest/posts} endpoint — the legacy {@code /v2/ugcPosts}
 * endpoint was deprecated by LinkedIn. For link shares, the {@code content} field
 * with {@code article} type replaces the old {@code specificContent.media} pattern.
 *
 * @see <a href="https://learn.microsoft.com/en-us/linkedin/marketing/community-management/shares/posts-api">LinkedIn Posts API</a>
 */
@ActionMapping(appKey = "linkedin", actionKey = "share-link")
public class LinkedInShareLinkHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInShareLinkHandler.class);
    private static final String POSTS_API = "https://api.linkedin.com/rest/posts";
    private static final String LINKEDIN_VERSION = "202401";
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null) return ActionResult.failure("LinkedIn requires an OAuth2 accessToken");

        String text = config.get("text") != null ? config.get("text").toString() : null;
        String linkUrl = config.get("linkUrl") != null ? config.get("linkUrl").toString() : null;
        if (text == null) return ActionResult.failure("'text' is required");
        if (linkUrl == null) return ActionResult.failure("'linkUrl' is required");

        // Get user URN via OIDC userinfo endpoint
        try {
            Map<String, Object> profile = restClient.get()
                    .uri("https://api.linkedin.com/v2/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            String sub = profile != null ? (String) profile.get("sub") : null;
            if (sub == null) return ActionResult.failure("Could not retrieve LinkedIn user ID");

            // Posts API payload with article content (replaces ugcPosts media format)
            Map<String, Object> body = new HashMap<>();
            body.put("author", "urn:li:person:" + sub);
            body.put("commentary", text);
            body.put("visibility", "PUBLIC");
            body.put("distribution", Map.of(
                    "feedDistribution", "MAIN_FEED"
            ));
            body.put("content", Map.of(
                    "article", Map.of(
                            "source", linkUrl
                    )
            ));
            body.put("lifecycleState", "PUBLISHED");

            String resp = restClient.post()
                    .uri(POSTS_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("LinkedIn-Version", LINKEDIN_VERSION)
                    .header("X-Restli-Protocol-Version", "2.0.0")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(String.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "linkedin");
            out.put("action", "share-link");
            out.put("response", resp);
            logger.info("[linkedin] Link shared successfully via Posts API");
            return ActionResult.success(out);
        } catch (Exception e) {
            logger.error("[linkedin] Share-link failed", e);
            return ActionResult.failure("LinkedIn share-link failed: " + e.getMessage());
        }
    }
}
