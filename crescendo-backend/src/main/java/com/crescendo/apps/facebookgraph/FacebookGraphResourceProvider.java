package com.crescendo.apps.facebookgraph;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.*;

/**
 * Provides dynamic dropdown resources for Facebook Graph API.
 * Uses URI.create to avoid URI template placeholder expansion on nested field syntax.
 * Uses String extraction + ObjectMapper to safely handle Meta's text/javascript responses.
 */
@Component
@SuppressWarnings("unchecked")
public class FacebookGraphResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(FacebookGraphResourceProvider.class);
    private static final String GRAPH_API_BASE = "https://graph.facebook.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FacebookGraphResourceProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        this.restClient = RestClient.builder()
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    @Override
    public String appKey() {
        return "facebook-graph";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("pages", "facebook_pages", "posts", "facebook_posts", "albums", "comments", "facebook_comments");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String token = extractToken(credentials);
        if (token == null || token.isBlank()) {
            logger.warn("[facebook-graph] No access token found in credentials for resourceType={}", resourceType);
            return List.of();
        }

        String version = credentials.get("graphVersion") != null && !credentials.get("graphVersion").toString().isBlank()
                ? credentials.get("graphVersion").toString().trim()
                : "v26.0";
        if (!version.startsWith("v")) version = "v" + version;

        return switch (resourceType) {
            case "pages", "facebook_pages" -> listPages(token, version);
            case "posts", "facebook_posts" -> listPosts(token, version, params != null ? params.get("pageId") : null);
            case "albums" -> listAlbums(token, version, params != null ? params.get("pageId") : null);
            case "comments", "facebook_comments" -> listComments(token, version, params != null ? params.get("postId") : null, params != null ? params.get("pageId") : null);
            default -> List.of();
        };
    }

    private static String enc(String val) {
        if (val == null) return "";
        return java.net.URLEncoder.encode(val, java.nio.charset.StandardCharsets.UTF_8);
    }

    private List<ResourceOption> listPosts(String token, String version, String pageId) {
        if (pageId == null || pageId.isBlank()) return List.of();
        try {
            String pageToken = getPageAccessToken(token, version, pageId);
            String effectiveToken = pageToken != null ? pageToken : token;
            String url = GRAPH_API_BASE + "/" + version + "/" + pageId + "/posts?fields=" + enc("id,message,created_time") + "&limit=50&access_token=" + enc(effectiveToken);
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.get("data") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        String id = String.valueOf(m.get("id"));
                        String message = m.get("message") != null ? String.valueOf(m.get("message")) : id;
                        String label = message.length() > 60 ? message.substring(0, 57) + "..." : message;
                        String createdTime = m.get("created_time") != null ? String.valueOf(m.get("created_time")) : "";
                        options.add(new ResourceOption(id, label, "Post " + createdTime));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[facebook-graph] Failed to list posts for page {}: {}", pageId, e.getMessage());
        }
        return List.of();
    }

    private List<ResourceOption> listComments(String token, String version, String postId, String pageId) {
        if (postId == null || postId.isBlank()) return List.of();
        try {
            String pageToken = pageId != null ? getPageAccessToken(token, version, pageId) : null;
            String effectiveToken = pageToken != null ? pageToken : token;
            String url = GRAPH_API_BASE + "/" + version + "/" + postId + "/comments?fields=" + enc("id,message,from,created_time") + "&limit=50&access_token=" + enc(effectiveToken);
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.get("data") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        String id = String.valueOf(m.get("id"));
                        String message = m.get("message") != null ? String.valueOf(m.get("message")) : id;
                        String label = message.length() > 60 ? message.substring(0, 57) + "..." : message;
                        String from = "";
                        if (m.get("from") instanceof Map<?, ?> f && f.get("name") != null) {
                            from = f.get("name") + ": ";
                        }
                        options.add(new ResourceOption(id, from + label, "Comment ID: " + id));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[facebook-graph] Failed to list comments for post {}: {}", postId, e.getMessage());
        }
        return List.of();
    }

    private List<ResourceOption> listAlbums(String token, String version, String pageId) {
        if (pageId == null || pageId.isBlank()) return List.of();
        try {
            String pageToken = getPageAccessToken(token, version, pageId);
            String effectiveToken = pageToken != null ? pageToken : token;
            String url = GRAPH_API_BASE + "/" + version + "/" + pageId + "/albums?fields=" + enc("id,name,count") + "&limit=50&access_token=" + enc(effectiveToken);
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.get("data") instanceof List<?> list) {
                List<ResourceOption> options = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        String id = String.valueOf(m.get("id"));
                        String name = m.get("name") != null ? String.valueOf(m.get("name")) : id;
                        String count = m.get("count") != null ? String.valueOf(m.get("count")) + " photos" : "Album";
                        options.add(new ResourceOption(id, name, count + " - ID: " + id));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[facebook-graph] Failed to list albums for page {}: {}", pageId, e.getMessage());
        }
        return List.of();
    }

    private String getPageAccessToken(String userToken, String version, String pageId) {
        try {
            String url = GRAPH_API_BASE + "/" + version + "/" + pageId + "?fields=access_token&access_token=" + enc(userToken);
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.get("access_token") != null) {
                return String.valueOf(resp.get("access_token"));
            }
        } catch (Exception e) {
            logger.debug("[facebook-graph] Could not get page access token for page {}: {}", pageId, e.getMessage());
        }
        return null;
    }

    private List<ResourceOption> listPages(String token, String version) {
        Map<String, ResourceOption> discoveredPages = new LinkedHashMap<>();

        // Strategy 1: /me/accounts
        try {
            String url = GRAPH_API_BASE + "/" + version + "/me/accounts?fields=" + enc("id,name,category,access_token,tasks") + "&limit=100&access_token=" + enc(token);
            logger.info("[facebook-graph] Strategy 1: GET /me/accounts");
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.containsKey("error")) {
                logger.warn("[facebook-graph] /me/accounts API error: {}", resp.get("error"));
            } else {
                collectPagesFromData(resp, discoveredPages);
            }
        } catch (Exception e) {
            logger.warn("[facebook-graph] /me/accounts failed: {}", e.getMessage());
        }

        // Strategy 2: /me?fields=accounts nested query
        if (discoveredPages.isEmpty()) {
            try {
                String url = GRAPH_API_BASE + "/" + version + "/me?fields=" + enc("id,name,accounts{id,name,category,access_token}") + "&access_token=" + enc(token);
                logger.info("[facebook-graph] Strategy 2: GET /me?fields=accounts");
                Map<String, Object> meResp = getJson(url);
                if (meResp != null) {
                    Object accounts = meResp.get("accounts");
                    if (accounts instanceof Map<?, ?> acctMap) {
                        collectPagesFromData((Map<String, Object>) acctMap, discoveredPages);
                    }
                }
            } catch (Exception e) {
                logger.warn("[facebook-graph] /me nested accounts query failed: {}", e.getMessage());
            }
        }

        // Strategy 3: debug_token to get page IDs from granular_scopes
        if (discoveredPages.isEmpty()) {
            try {
                String url = GRAPH_API_BASE + "/" + version + "/debug_token?input_token=" + enc(token) + "&access_token=" + enc(token);
                logger.info("[facebook-graph] Strategy 3: GET /debug_token granular_scopes");
                Map<String, Object> debugResp = getJson(url);
                if (debugResp != null && debugResp.get("data") instanceof Map<?, ?> dMap) {
                    Object granularScopes = dMap.get("granular_scopes");
                    if (granularScopes instanceof List<?> scopeList) {
                        for (Object sObj : scopeList) {
                            if (sObj instanceof Map<?, ?> sMap && sMap.get("target_ids") instanceof List<?> idList) {
                                for (Object idObj : idList) {
                                    String targetId = String.valueOf(idObj);
                                    if (!discoveredPages.containsKey(targetId)) {
                                        fetchPageById(targetId, token, version, discoveredPages);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("[facebook-graph] debug_token granular_scopes check failed: {}", e.getMessage());
            }
        }

        if (discoveredPages.isEmpty()) {
            logger.warn("[facebook-graph] No Facebook Pages found. Ensure pages_show_list was granted. Disconnect and reconnect to re-prompt permissions.");
        } else {
            logger.info("[facebook-graph] Discovered {} page(s)", discoveredPages.size());
        }
        return new ArrayList<>(discoveredPages.values());
    }

    private void collectPagesFromData(Map<String, Object> response, Map<String, ResourceOption> out) {
        if (response == null || !response.containsKey("data")) return;
        Object dataObj = response.get("data");
        if (!(dataObj instanceof List<?> list)) return;
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                String id = String.valueOf(map.get("id"));
                String name = map.get("name") != null ? String.valueOf(map.get("name")) : id;
                String category = map.get("category") != null ? String.valueOf(map.get("category")) : "Page";
                out.putIfAbsent(id, new ResourceOption(id, name, category + " - ID: " + id));
                logger.info("[facebook-graph] Found page: {} ({})", name, id);
            }
        }
    }

    private void fetchPageById(String pageId, String token, String version, Map<String, ResourceOption> out) {
        try {
            String url = GRAPH_API_BASE + "/" + version + "/" + pageId + "?fields=id,name,category&access_token=" + token;
            Map<String, Object> pageResp = getJson(url);
            if (pageResp != null && pageResp.containsKey("id")) {
                String id = String.valueOf(pageResp.get("id"));
                String name = pageResp.get("name") != null ? String.valueOf(pageResp.get("name")) : id;
                String category = pageResp.get("category") != null ? String.valueOf(pageResp.get("category")) : "Page";
                out.put(id, new ResourceOption(id, name, category + " - ID: " + id));
                logger.info("[facebook-graph] Fetched page by ID: {} ({})", name, id);
            }
        } catch (Exception e) {
            logger.debug("[facebook-graph] Failed to fetch page {}: {}", pageId, e.getMessage());
        }
    }

    private Map<String, Object> getJson(String url) {
        try {
            String raw = restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(String.class);
            if (raw == null || raw.isBlank()) return Map.of();
            return objectMapper.readValue(raw, Map.class);
        } catch (Exception e) {
            logger.warn("[facebook-graph] HTTP request failed: {} -> {}", url, e.getMessage());
            return Map.of();
        }
    }

    private String extractToken(Map<String, Object> credentials) {
        if (credentials == null) return null;
        Object token = credentials.get("accessToken");
        if (token == null) token = credentials.get("pageAccessToken");
        return token != null ? token.toString().trim() : null;
    }
}

