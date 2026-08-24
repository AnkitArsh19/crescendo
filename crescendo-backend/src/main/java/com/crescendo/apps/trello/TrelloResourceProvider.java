package com.crescendo.apps.trello;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class TrelloResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(TrelloResourceProvider.class);
    private static final String BASE_URL = "https://api.trello.com/1";

    @Override
    public String appKey() {
        return "trello";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("boards", "lists", "cards");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String apiKey = extract(credentials, "apiKey", "key", "clientId");
        String apiToken = extract(credentials, "apiToken", "token", "accessToken");

        if (apiKey == null || apiToken == null) {
            logger.warn("[trello] Missing apiKey or apiToken for resource listing");
            return List.of();
        }

        return switch (resourceType) {
            case "boards" -> listBoards(apiKey, apiToken);
            case "lists" -> listLists(apiKey, apiToken, params != null ? params.get("boardId") : null);
            case "cards" -> listCards(apiKey, apiToken, params != null ? params.get("boardId") : null);
            default -> List.of();
        };
    }

    private String extract(Map<String, Object> credentials, String... keys) {
        if (credentials == null) return null;
        for (String k : keys) {
            Object v = credentials.get(k);
            if (v != null && !v.toString().isBlank()) return v.toString();
        }
        return null;
    }

    private RestClient createClient() {
        return RestClient.builder().baseUrl(BASE_URL).build();
    }

    private List<ResourceOption> listBoards(String apiKey, String apiToken) {
        try {
            List<Map<String, Object>> boards = createClient().get()
                    .uri("/members/me/boards?key=" + apiKey + "&token=" + apiToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (boards == null) return List.of();
            List<ResourceOption> options = new ArrayList<>();
            for (Map<String, Object> b : boards) {
                String id = String.valueOf(b.get("id"));
                String name = String.valueOf(b.get("name"));
                options.add(new ResourceOption(id, name));
            }
            return options;
        } catch (Exception e) {
            logger.warn("[trello] Error fetching boards: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listLists(String apiKey, String apiToken, String boardId) {
        if (boardId == null || boardId.isBlank()) return List.of();
        try {
            List<Map<String, Object>> lists = createClient().get()
                    .uri("/boards/" + boardId + "/lists?key=" + apiKey + "&token=" + apiToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (lists == null) return List.of();
            List<ResourceOption> options = new ArrayList<>();
            for (Map<String, Object> l : lists) {
                String id = String.valueOf(l.get("id"));
                String name = String.valueOf(l.get("name"));
                options.add(new ResourceOption(id, name));
            }
            return options;
        } catch (Exception e) {
            logger.warn("[trello] Error fetching lists: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listCards(String apiKey, String apiToken, String boardId) {
        if (boardId == null || boardId.isBlank()) return List.of();
        try {
            List<Map<String, Object>> cards = createClient().get()
                    .uri("/boards/" + boardId + "/cards?key=" + apiKey + "&token=" + apiToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (cards == null) return List.of();
            List<ResourceOption> options = new ArrayList<>();
            for (Map<String, Object> c : cards) {
                String id = String.valueOf(c.get("id"));
                String name = String.valueOf(c.get("name"));
                options.add(new ResourceOption(id, name));
            }
            return options;
        } catch (Exception e) {
            logger.warn("[trello] Error fetching cards: {}", e.getMessage());
            return List.of();
        }
    }
}
