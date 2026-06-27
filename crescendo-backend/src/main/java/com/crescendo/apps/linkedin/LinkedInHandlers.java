package com.crescendo.apps.linkedin;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LinkedInHandlers {

    private static final String LINKEDIN_API = "https://api.linkedin.com/v2";
    private static final String POSTS_API = "https://api.linkedin.com/rest/posts";
    private static final String LINKEDIN_VERSION = "202401";

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    private String getPersonUrn(ActionContext context) {
        return context.getCredential("personUrn");
    }

    @ActionMapping(appKey = "linkedin", actionKey = "get-profile")
    public Object getProfile(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(LINKEDIN_API + "/me?projection=(id,firstName,lastName,profilePicture)")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "linkedin", actionKey = "share-post")
    public Object sharePost(ActionContext context) throws Exception {
        String text = context.getString("text");
        String visibility = (String) context.configuration().getOrDefault("visibility", "PUBLIC");
        
        Map<String, Object> body = new HashMap<>();
        body.put("author", getPersonUrn(context));
        body.put("commentary", text);
        body.put("visibility", visibility);
        body.put("distribution", Map.of("feedDistribution", "MAIN_FEED"));
        body.put("lifecycleState", "PUBLISHED");

        return RestClient.builder()
                .url(POSTS_API)
                .header("Authorization", getAuth(context))
                .header("LinkedIn-Version", LINKEDIN_VERSION)
                .header("X-Restli-Protocol-Version", "2.0.0")
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "linkedin", actionKey = "share-link")
    public Object shareLink(ActionContext context) throws Exception {
        String text = context.getString("text");
        String link = context.getString("linkUrl");
        String linkTitle = context.getString("linkTitle");
        String linkDesc = context.getString("linkDescription");
        String visibility = (String) context.configuration().getOrDefault("visibility", "PUBLIC");
        
        // This simulates a link share using the Posts API format
        Map<String, Object> body = new HashMap<>();
        body.put("author", getPersonUrn(context));
        body.put("commentary", text);
        body.put("visibility", visibility);
        body.put("distribution", Map.of("feedDistribution", "MAIN_FEED"));
        body.put("lifecycleState", "PUBLISHED");
        
        Map<String, Object> article = new HashMap<>();
        article.put("source", link);
        if (linkTitle != null) article.put("title", linkTitle);
        if (linkDesc != null) article.put("description", linkDesc);
        
        body.put("content", Map.of("article", article));

        return RestClient.builder()
                .url(POSTS_API)
                .header("Authorization", getAuth(context))
                .header("LinkedIn-Version", LINKEDIN_VERSION)
                .header("X-Restli-Protocol-Version", "2.0.0")
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }
}
