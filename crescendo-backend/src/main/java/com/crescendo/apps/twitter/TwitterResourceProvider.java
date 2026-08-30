package com.crescendo.apps.twitter;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Twitter/X resources: user tweets and profile.
 * Authenticates via OAuth2 access token or Bearer token.
 */
@Component
@SuppressWarnings("unchecked")
public class TwitterResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(TwitterResourceProvider.class);
    private static final String TWITTER_API = "https://api.twitter.com/2";

    private final RestClient restClient;

    public TwitterResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "twitter";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("tweets", "users");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of();
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String token = getToken(credentials);
        return switch (resourceType) {
            case "tweets" -> listMyTweets(token);
            case "users"  -> listMe(token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listMyTweets(String token) {
        try {
            // First get authenticated user ID
            Map<String, Object> meResp = restClient.get()
                    .uri(TWITTER_API + "/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (meResp == null || !meResp.containsKey("data")) return List.of();
            Map<String, Object> data = (Map<String, Object>) meResp.get("data");
            if (data == null) return List.of();
            String userId = str(data.get("id"));

            Map<String, Object> tweetsResp = restClient.get()
                    .uri(TWITTER_API + "/users/" + userId + "/tweets?max_results=20")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (tweetsResp == null || !tweetsResp.containsKey("data")) return List.of();
            List<Map<String, Object>> tweets = (List<Map<String, Object>>) tweetsResp.get("data");
            if (tweets == null) return List.of();

            return tweets.stream().map(t -> {
                String id = str(t.get("id"));
                String text = str(t.get("text"));
                String label = text.length() > 50 ? text.substring(0, 47) + "..." : text;
                return new ResourceOption(id, label, "ID: " + id);
            }).toList();
        } catch (Exception e) {
            logger.error("[twitter] Failed to list tweets: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listMe(String token) {
        try {
            Map<String, Object> meResp = restClient.get()
                    .uri(TWITTER_API + "/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (meResp == null || !meResp.containsKey("data")) return List.of();
            Map<String, Object> data = (Map<String, Object>) meResp.get("data");
            if (data == null) return List.of();
            String userId = str(data.get("id"));
            String name = str(data.get("name"));
            String username = str(data.get("username"));
            return List.of(new ResourceOption(userId, name + " (@" + username + ")", "User ID: " + userId));
        } catch (Exception e) {
            logger.error("[twitter] Failed to get user profile: {}", e.getMessage());
            return List.of();
        }
    }

    private String getToken(Map<String, Object> credentials) {
        if (credentials != null) {
            for (String key : new String[]{"accessToken", "token", "bearerToken", "apiKey"}) {
                Object val = credentials.get(key);
                if (val != null && !val.toString().isBlank()) return val.toString();
            }
        }
        throw new IllegalArgumentException("Twitter requires an access token or bearer token.");
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
