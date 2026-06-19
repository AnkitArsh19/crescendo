package com.crescendo.apps.hackernews;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@SuppressWarnings("unchecked")
class HackerNewsClient {

    private static final String BASE_URL = "https://hacker-news.firebaseio.com/v0";

    private final RestClient restClient = RestClient.create(BASE_URL);
    private final ObjectMapper objectMapper;

    HackerNewsClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    Map<String, Object> getItem(long itemId) throws Exception {
        String response = restClient.get()
                .uri("/item/{itemId}.json", itemId)
                .retrieve()
                .body(String.class);
        Map<String, Object> parsed = objectMapper.readValue(response, Map.class);
        return parsed != null ? parsed : Map.of();
    }

    Map<String, Object> getUser(String username) throws Exception {
        String response = restClient.get()
                .uri("/user/{username}.json", username)
                .retrieve()
                .body(String.class);
        Map<String, Object> parsed = objectMapper.readValue(response, Map.class);
        return parsed != null ? parsed : Map.of();
    }

    Long[] getTopStoryIds() {
        return restClient.get()
                .uri("/topstories.json")
                .retrieve()
                .body(Long[].class);
    }
}
