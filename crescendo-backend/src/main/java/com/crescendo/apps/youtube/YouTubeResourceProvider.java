package com.crescendo.apps.youtube;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class YouTubeResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeResourceProvider.class);
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";

    private static final List<ResourceOption> STANDARD_CATEGORIES = List.of(
            new ResourceOption("1", "Film & Animation"),
            new ResourceOption("2", "Autos & Vehicles"),
            new ResourceOption("10", "Music"),
            new ResourceOption("15", "Pets & Animals"),
            new ResourceOption("17", "Sports"),
            new ResourceOption("19", "Travel & Events"),
            new ResourceOption("20", "Gaming"),
            new ResourceOption("22", "People & Blogs"),
            new ResourceOption("23", "Comedy"),
            new ResourceOption("24", "Entertainment"),
            new ResourceOption("25", "News & Politics"),
            new ResourceOption("26", "Howto & Style"),
            new ResourceOption("27", "Education"),
            new ResourceOption("28", "Science & Technology"),
            new ResourceOption("29", "Nonprofits & Activism")
    );

    @Override
    public String appKey() {
        return "youtube";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("videoCategories", "video_categories", "playlists", "channels");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String token = credentials != null && credentials.get("accessToken") != null
                ? credentials.get("accessToken").toString()
                : null;
        String apiKey = credentials != null && credentials.get("apiKey") != null
                ? credentials.get("apiKey").toString()
                : null;

        return switch (resourceType) {
            case "videoCategories", "video_categories" -> listCategories(token, apiKey);
            case "playlists" -> listPlaylists(token, apiKey);
            case "channels" -> listChannels(token, apiKey);
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listCategories(String token, String apiKey) {
        if (token == null && apiKey == null) {
            return STANDARD_CATEGORIES;
        }
        try {
            RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
            if (token != null) {
                builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            String uri = apiKey != null
                    ? "/videoCategories?part=snippet&regionCode=US&key=" + apiKey
                    : "/videoCategories?part=snippet&regionCode=US";

            Map<String, Object> resp = builder.build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("items") instanceof List<?> items) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : items) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        Map<?, ?> snippet = (Map<?, ?>) item.get("snippet");
                        String title = snippet != null ? String.valueOf(snippet.get("title")) : id;
                        options.add(new ResourceOption(id, title));
                    }
                }
                if (!options.isEmpty()) return options;
            }
        } catch (Exception e) {
            logger.warn("[youtube] Error fetching live categories, falling back to standard: {}", e.getMessage());
        }
        return STANDARD_CATEGORIES;
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listPlaylists(String token, String apiKey) {
        if (token == null) return List.of();
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();

            Map<String, Object> resp = client.get()
                    .uri("/playlists?part=snippet&mine=true&maxResults=50")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("items") instanceof List<?> items) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : items) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        Map<?, ?> snippet = (Map<?, ?>) item.get("snippet");
                        String title = snippet != null ? String.valueOf(snippet.get("title")) : id;
                        options.add(new ResourceOption(id, title));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[youtube] Error fetching playlists: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listChannels(String token, String apiKey) {
        if (token == null) return List.of();
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();

            Map<String, Object> resp = client.get()
                    .uri("/channels?part=snippet&mine=true")
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("items") instanceof List<?> items) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object itemObj : items) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String id = String.valueOf(item.get("id"));
                        Map<?, ?> snippet = (Map<?, ?>) item.get("snippet");
                        String title = snippet != null ? String.valueOf(snippet.get("title")) : id;
                        options.add(new ResourceOption(id, title));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[youtube] Error fetching channels: {}", e.getMessage());
        }
        return List.of();
    }
}
