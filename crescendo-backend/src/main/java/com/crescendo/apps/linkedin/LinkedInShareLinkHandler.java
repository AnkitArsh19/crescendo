package com.crescendo.apps.linkedin;

import com.crescendo.execution.action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Shares a post with a link attachment on LinkedIn via Community Management API.
 */
@ActionMapping(appKey = "linkedin", actionKey = "share-link")
public class LinkedInShareLinkHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(LinkedInShareLinkHandler.class);
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

        // Get user URN first
        try {
            Map<String, Object> profile = restClient.get()
                    .uri("https://api.linkedin.com/v2/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            String sub = profile != null ? (String) profile.get("sub") : null;
            if (sub == null) return ActionResult.failure("Could not retrieve LinkedIn user ID");

            Map<String, Object> body = new HashMap<>();
            body.put("author", "urn:li:person:" + sub);
            body.put("lifecycleState", "PUBLISHED");
            body.put("specificContent", Map.of("com.linkedin.ugc.ShareContent", Map.of(
                "shareCommentary", Map.of("text", text),
                "shareMediaCategory", "ARTICLE",
                "media", List.of(Map.of(
                    "status", "READY",
                    "originalUrl", linkUrl
                ))
            )));
            body.put("visibility", Map.of("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC"));

            String resp = restClient.post()
                    .uri("https://api.linkedin.com/v2/ugcPosts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(String.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "linkedin");
            out.put("action", "share-link");
            out.put("response", resp);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("LinkedIn share-link failed: " + e.getMessage());
        }
    }
}
