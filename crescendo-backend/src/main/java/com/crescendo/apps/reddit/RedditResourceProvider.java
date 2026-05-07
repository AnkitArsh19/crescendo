package com.crescendo.apps.reddit;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Reddit resources: subscribed subreddits.
 * Uses the Reddit OAuth API (oauth.reddit.com).
 */
@Component
public class RedditResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(RedditResourceProvider.class);
    private static final String REDDIT_API = "https://oauth.reddit.com";

    @Override
    public String appKey() {
        return "reddit";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("subreddits");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = credentials.get("accessToken").toString();
        return listSubreddits(accessToken);
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listSubreddits(String accessToken) {
        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(REDDIT_API + "/subreddits/mine/subscriber?limit=100")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) return List.of();

            List<Map<String, Object>> children = (List<Map<String, Object>>) data.get("children");
            if (children == null) return List.of();

            return children.stream()
                    .map(child -> {
                        Map<String, Object> sub = (Map<String, Object>) child.get("data");
                        return new ResourceOption(
                                sub.get("display_name").toString(),
                                "r/" + sub.get("display_name"),
                                sub.get("subscribers") != null ? sub.get("subscribers") + " members" : null
                        );
                    })
                    .sorted(Comparator.comparing(ResourceOption::label))
                    .toList();
        } catch (Exception e) {
            logger.error("[reddit] Failed to list subreddits: {}", e.getMessage());
            return List.of();
        }
    }

    private RestClient restClient(String accessToken) {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "Crescendo/1.0")
                .build();
    }
}
