package com.crescendo.apps.facebookgraph;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@SuppressWarnings("unchecked")
public class FacebookGraphHandlers {

    private static final Logger logger = LoggerFactory.getLogger(FacebookGraphHandlers.class);
    private static final String DEFAULT_VERSION = "v26.0";
    private static final String GRAPH_BASE = "https://graph.facebook.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FacebookGraphHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        this.restClient = RestClient.builder()
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    private String getVersion(ActionContext context) {
        String ver = (String) context.credentials().get("graphVersion");
        if (ver == null || ver.isBlank()) ver = DEFAULT_VERSION;
        if (!ver.startsWith("v")) ver = "v" + ver;
        return ver;
    }

    private static String enc(String val) {
        if (val == null) return "";
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }

    /**
     * Resolves the Page Access Token for a given pageId.
     * Meta requires the Page Access Token (not User Access Token) to post to a page feed.
     */
    private String getEffectivePageToken(ActionContext context, String pageId) {
        Object directPageToken = context.credentials().get("pageAccessToken");
        if (directPageToken != null && !directPageToken.toString().isBlank()) {
            return directPageToken.toString().trim();
        }

        String userToken = (String) context.credentials().get("accessToken");
        if (userToken == null || userToken.isBlank()) {
            return "";
        }
        if (pageId == null || pageId.isBlank()) {
            return userToken;
        }

        String version = getVersion(context);

        // Strategy 1: GET /{pageId}?fields=access_token&access_token={userToken}
        try {
            String url = GRAPH_BASE + "/" + version + "/" + pageId + "?fields=" + enc("access_token") + "&access_token=" + enc(userToken);
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.get("access_token") != null) {
                String token = String.valueOf(resp.get("access_token"));
                logger.info("[facebook-graph] Successfully obtained Page Access Token for page: {}", pageId);
                return token;
            }
        } catch (Exception e) {
            logger.debug("[facebook-graph] Strategy 1 for page token failed: {}", e.getMessage());
        }

        // Strategy 2: GET /me/accounts?fields=id,access_token&access_token={userToken}
        try {
            String url = GRAPH_BASE + "/" + version + "/me/accounts?fields=" + enc("id,access_token") + "&limit=100&access_token=" + enc(userToken);
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.get("data") instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m && pageId.equals(String.valueOf(m.get("id")))) {
                        if (m.get("access_token") != null) {
                            String token = String.valueOf(m.get("access_token"));
                            logger.info("[facebook-graph] Found Page Access Token in /me/accounts for page: {}", pageId);
                            return token;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("[facebook-graph] Strategy 2 for page token failed: {}", e.getMessage());
        }

        logger.warn("[facebook-graph] Could not resolve Page Access Token for page {}. Falling back to User token.", pageId);
        return userToken;
    }

    private static String extractUrl(Object val) {
        if (val == null) return "";
        if (val instanceof Map<?, ?> m) {
            Object url = m.get("url");
            if (url != null) return url.toString().trim();
        }
        return val.toString().trim();
    }

    @ActionMapping(appKey = "facebook-graph", actionKey = "create-page-post")
    public Object createPost(ActionContext context) throws Exception {
        String pageId = context.configuration().get("pageId") != null ? context.configuration().get("pageId").toString().trim() : "";
        String message = context.configuration().get("message") != null ? context.configuration().get("message").toString() : "";
        String link = context.configuration().get("link") != null ? context.configuration().get("link").toString().trim() : "";
        String imageUrl = extractUrl(context.configuration().get("imageUrl"));
        String publishedStr = context.configuration().get("published") != null ? context.configuration().get("published").toString().trim() : "true";

        if (pageId.isBlank() || message.isBlank()) {
            return ActionResult.failure("Facebook Page and Message are required");
        }

        String effectiveToken = getEffectivePageToken(context, pageId);
        String version = getVersion(context);

        try {
            // Photo post if image provided
            if (!imageUrl.isBlank()) {
                Map<String, Object> photoBody = new HashMap<>();
                photoBody.put("url", imageUrl);
                photoBody.put("caption", message);
                photoBody.put("published", Boolean.parseBoolean(publishedStr));
                photoBody.put("access_token", effectiveToken);

                String url = GRAPH_BASE + "/" + version + "/" + pageId + "/photos?access_token=" + enc(effectiveToken);
                String raw = restClient.post()
                        .uri(URI.create(url))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(photoBody)
                        .retrieve()
                        .body(String.class);

                Map<String, Object> result = objectMapper.readValue(raw, Map.class);
                return ActionResult.success(result);
            }

            // Standard feed post
            Map<String, Object> body = new HashMap<>();
            body.put("message", message);
            if (!link.isBlank()) {
                body.put("link", link);
            }
            body.put("published", Boolean.parseBoolean(publishedStr));
            body.put("access_token", effectiveToken);

            String url = GRAPH_BASE + "/" + version + "/" + pageId + "/feed?access_token=" + enc(effectiveToken);
            String raw = restClient.post()
                    .uri(URI.create(url))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> result = objectMapper.readValue(raw, Map.class);
            return ActionResult.success(result);
        } catch (Exception e) {
            return ActionResult.failure("Failed to create Facebook Page post: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "facebook-graph", actionKey = "create-page-photo")
    public Object createPhoto(ActionContext context) throws Exception {
        String pageId = context.configuration().get("pageId") != null ? context.configuration().get("pageId").toString().trim() : "";
        String imageUrl = extractUrl(context.configuration().get("imageUrl"));
        String caption = context.configuration().get("caption") != null ? context.configuration().get("caption").toString() : "";

        if (pageId.isBlank() || imageUrl.isBlank()) {
            return ActionResult.failure("Facebook Page and Photo URL are required");
        }

        String effectiveToken = getEffectivePageToken(context, pageId);
        String version = getVersion(context);

        try {
            Map<String, Object> photoBody = new HashMap<>();
            photoBody.put("url", imageUrl);
            if (!caption.isBlank()) {
                photoBody.put("caption", caption);
            }
            photoBody.put("access_token", effectiveToken);

            String url = GRAPH_BASE + "/" + version + "/" + pageId + "/photos?access_token=" + enc(effectiveToken);
            String raw = restClient.post()
                    .uri(URI.create(url))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(photoBody)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> result = objectMapper.readValue(raw, Map.class);
            return ActionResult.success(result);
        } catch (Exception e) {
            return ActionResult.failure("Failed to upload Facebook Page photo: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "facebook-graph", actionKey = "create-post-comment")
    public Object createComment(ActionContext context) throws Exception {
        String pageId = context.configuration().get("pageId") != null ? context.configuration().get("pageId").toString().trim() : "";
        String postId = context.configuration().get("postId") != null ? context.configuration().get("postId").toString().trim() : "";
        String message = context.configuration().get("message") != null ? context.configuration().get("message").toString() : "";

        if (postId.isBlank() || message.isBlank()) {
            return ActionResult.failure("Post ID and Comment Text are required");
        }

        String effectiveToken = getEffectivePageToken(context, pageId);
        String version = getVersion(context);

        try {
            String url = GRAPH_BASE + "/" + version + "/" + postId + "/comments?access_token=" + enc(effectiveToken);
            String raw = restClient.post()
                    .uri(URI.create(url))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", message, "access_token", effectiveToken))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> result = objectMapper.readValue(raw, Map.class);
            return ActionResult.success(result);
        } catch (Exception e) {
            return ActionResult.failure("Failed to comment on Facebook post: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "facebook-graph", actionKey = "get-page-posts")
    public Object getPagePosts(ActionContext context) throws Exception {
        String pageId = context.configuration().get("pageId") != null ? context.configuration().get("pageId").toString().trim() : "";
        String limit = context.configuration().get("limit") != null ? context.configuration().get("limit").toString().trim() : "25";

        if (pageId.isBlank()) {
            return ActionResult.failure("Facebook Page is required");
        }

        String effectiveToken = getEffectivePageToken(context, pageId);
        String version = getVersion(context);

        try {
            String url = GRAPH_BASE + "/" + version + "/" + pageId + "/posts?fields=" + enc("id,message,created_time,full_picture,permalink_url,shares") + "&limit=" + enc(limit) + "&access_token=" + enc(effectiveToken);
            Map<String, Object> result = getJson(url);
            return ActionResult.success(result);
        } catch (Exception e) {
            return ActionResult.failure("Failed to get Facebook Page posts: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "facebook-graph", actionKey = "get-page-insights")
    public Object getPageInsights(ActionContext context) throws Exception {
        String pageId = context.configuration().get("pageId") != null ? context.configuration().get("pageId").toString().trim() : "";

        if (pageId.isBlank()) {
            return ActionResult.failure("Facebook Page is required");
        }

        String effectiveToken = getEffectivePageToken(context, pageId);
        String version = getVersion(context);

        try {
            String url = GRAPH_BASE + "/" + version + "/" + pageId + "/insights?metric=" + enc("page_impressions,page_engaged_users,page_fans") + "&period=day&access_token=" + enc(effectiveToken);
            Map<String, Object> result = getJson(url);
            return ActionResult.success(result);
        } catch (Exception e) {
            return ActionResult.failure("Failed to get Facebook Page insights: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "facebook-graph", actionKey = "get-node")
    public Object getNode(ActionContext context) throws Exception {
        String nodeId = context.configuration().get("nodeId") != null ? context.configuration().get("nodeId").toString().trim() : "";
        String fields = context.configuration().get("fields") != null ? context.configuration().get("fields").toString().trim() : "";
        String userToken = (String) context.credentials().get("accessToken");

        if (nodeId.isBlank()) {
            return ActionResult.failure("Node ID is required");
        }

        String version = getVersion(context);

        try {
            String fieldsPart = !fields.isBlank() ? "?fields=" + enc(fields) + "&access_token=" + enc(userToken) : "?access_token=" + enc(userToken);
            String url = GRAPH_BASE + "/" + version + "/" + nodeId + fieldsPart;
            Map<String, Object> result = getJson(url);
            return ActionResult.success(result);
        } catch (Exception e) {
            return ActionResult.failure("Failed to fetch Facebook Graph node: " + e.getMessage());
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
            logger.warn("[facebook-graph] HTTP GET failed: {} -> {}", url, e.getMessage());
            return Map.of();
        }
    }
}

