package com.crescendo.apps.instagram;

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
 * Provides dynamic dropdown resources for Instagram Graph API.
 * Uses URI.create to avoid URI template placeholder expansion on nested field syntax.
 * Uses String extraction + ObjectMapper to safely handle Meta's text/javascript responses.
 */
@Component
@SuppressWarnings("unchecked")
public class InstagramResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(InstagramResourceProvider.class);
    private static final String GRAPH_FB_BASE = "https://graph.facebook.com";
    private static final String GRAPH_IG_BASE = "https://graph.instagram.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public InstagramResourceProvider(ObjectMapper objectMapper) {
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
        return "instagram";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("accounts", "instagram_accounts", "users", "media", "posts", "comments", "conversations", "recipients");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String token = extractToken(credentials);
        if (token == null || token.isBlank()) {
            logger.warn("[instagram] No access token in credentials for resourceType={}", resourceType);
            return List.of();
        }

        String version = credentials.get("graphVersion") != null && !credentials.get("graphVersion").toString().isBlank()
                ? credentials.get("graphVersion").toString().trim()
                : "v26.0";
        if (!version.startsWith("v")) version = "v" + version;

        return switch (resourceType) {
            case "accounts", "instagram_accounts", "users" -> listInstagramAccounts(token, version);
            case "media", "posts" -> listUserMedia(token, version, params != null ? params.get("igUserId") : null);
            case "comments" -> listMediaComments(token, version, params != null ? params.get("mediaId") : null);
            case "conversations", "recipients" -> listConversations(token, version, params != null ? params.get("igUserId") : null);
            default -> List.of();
        };
    }

    private static String enc(String val) {
        if (val == null) return "";
        return java.net.URLEncoder.encode(val, java.nio.charset.StandardCharsets.UTF_8);
    }

    private List<ResourceOption> listInstagramAccounts(String token, String version) {
        // Strategy 1: Instagram Login token — works at graph.instagram.com/me
        try {
            String url = GRAPH_IG_BASE + "/me?fields=" + enc("id,username,name,account_type,media_count") + "&access_token=" + enc(token);
            logger.info("[instagram] Strategy 1: GET graph.instagram.com/me");
            Map<String, Object> me = getJson(url);
            if (me != null && me.containsKey("id")) {
                String id = String.valueOf(me.get("id"));
                String username = me.get("username") != null ? "@" + me.get("username") : id;
                String type = me.get("account_type") != null ? String.valueOf(me.get("account_type")) : "Instagram Account";
                logger.info("[instagram] Found account via Instagram Login: {} ({})", username, id);
                return List.of(new ResourceOption(id, username, type + " - ID: " + id));
            }
            if (me != null && me.containsKey("error")) {
                logger.warn("[instagram] graph.instagram.com/me error: {}", me.get("error"));
            }
        } catch (Exception e) {
            logger.warn("[instagram] graph.instagram.com/me failed: {}", e.getMessage());
        }

        // Strategy 2: Facebook Graph API with instagram_business_account field
        try {
            String url = GRAPH_FB_BASE + "/" + version + "/me/accounts?fields=" + enc("id,name,instagram_business_account{id,name,username,account_type}") + "&limit=100&access_token=" + enc(token);
            logger.info("[instagram] Strategy 2: GET graph.facebook.com/{}/me/accounts for instagram_business_account", version);
            Map<String, Object> response = getJson(url);

            if (response != null && response.containsKey("error")) {
                logger.warn("[instagram] /me/accounts error: {}", response.get("error"));
            } else if (response != null && response.get("data") instanceof List<?> pages) {
                logger.info("[instagram] /me/accounts returned {} pages", pages.size());
                List<ResourceOption> options = new ArrayList<>();
                for (Object pageObj : pages) {
                    if (pageObj instanceof Map<?, ?> page && page.get("instagram_business_account") instanceof Map igAccount) {
                        String igId = String.valueOf(igAccount.get("id"));
                        String username = igAccount.get("username") != null ? "@" + igAccount.get("username") : igId;
                        String pageName = page.get("name") != null ? String.valueOf(page.get("name")) : "Linked Page";
                        options.add(new ResourceOption(igId, username, "Linked to: " + pageName));
                        logger.info("[instagram] Found Instagram account: {} linked to page {}", username, pageName);
                    }
                }
                if (!options.isEmpty()) return options;
            }
        } catch (Exception e) {
            logger.warn("[instagram] /me/accounts instagram_business_account failed: {}", e.getMessage());
        }

        logger.warn("[instagram] No Instagram accounts found.");
        return List.of();
    }

    private List<ResourceOption> listUserMedia(String token, String version, String igUserId) {
        String targetId = (igUserId != null && !igUserId.isBlank()) ? igUserId : "me";
        List<ResourceOption> options = new ArrayList<>();

        // Strategy 1: graph.instagram.com/{targetId}/media
        try {
            String url = GRAPH_IG_BASE + "/" + targetId + "/media?fields=" + enc("id,caption,media_type,timestamp,permalink") + "&limit=50&access_token=" + enc(token);
            logger.info("[instagram] Fetching media from graph.instagram.com/{}/media", targetId);
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.get("data") instanceof List<?> list && !list.isEmpty()) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        String id = String.valueOf(m.get("id"));
                        String caption = m.get("caption") != null ? String.valueOf(m.get("caption")) : id;
                        String label = caption.length() > 60 ? caption.substring(0, 57) + "..." : caption;
                        String type = m.get("media_type") != null ? String.valueOf(m.get("media_type")) : "POST";
                        String time = m.get("timestamp") != null ? String.valueOf(m.get("timestamp")) : "";
                        options.add(new ResourceOption(id, label, type + " • " + time + " • ID: " + id));
                    }
                }
                logger.info("[instagram] Found {} media items via graph.instagram.com", options.size());
                return options;
            }
        } catch (Exception e) {
            logger.debug("[instagram] graph.instagram.com media query failed: {}", e.getMessage());
        }

        // Strategy 2: graph.facebook.com/{version}/{targetId}/media
        try {
            String url = GRAPH_FB_BASE + "/" + version + "/" + targetId + "/media?fields=" + enc("id,caption,media_type,timestamp,permalink") + "&limit=50&access_token=" + enc(token);
            logger.info("[instagram] Fetching media from graph.facebook.com/{}/{}/media", version, targetId);
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.get("data") instanceof List<?> list && !list.isEmpty()) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        String id = String.valueOf(m.get("id"));
                        String caption = m.get("caption") != null ? String.valueOf(m.get("caption")) : id;
                        String label = caption.length() > 60 ? caption.substring(0, 57) + "..." : caption;
                        String type = m.get("media_type") != null ? String.valueOf(m.get("media_type")) : "POST";
                        String time = m.get("timestamp") != null ? String.valueOf(m.get("timestamp")) : "";
                        options.add(new ResourceOption(id, label, type + " • " + time + " • ID: " + id));
                    }
                }
                logger.info("[instagram] Found {} media items via graph.facebook.com", options.size());
                return options;
            }
        } catch (Exception e) {
            logger.warn("[instagram] Failed to list media for user {}: {}", targetId, e.getMessage());
        }

        return options;
    }

    private List<ResourceOption> listMediaComments(String token, String version, String mediaId) {
        if (mediaId == null || mediaId.isBlank()) return List.of();
        List<ResourceOption> options = new ArrayList<>();

        // Strategy 1: graph.instagram.com/{mediaId}/comments
        try {
            String url = GRAPH_IG_BASE + "/" + mediaId + "/comments?fields=" + enc("id,text,username,timestamp,from,like_count") + "&limit=50&access_token=" + enc(token);
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.get("data") instanceof List<?> list && !list.isEmpty()) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        String id = String.valueOf(m.get("id"));
                        String text = m.get("text") != null ? String.valueOf(m.get("text")) : id;
                        String username = m.get("username") != null ? "@" + m.get("username") : "";
                        String label = username + (!username.isBlank() ? ": " : "") + (text.length() > 50 ? text.substring(0, 47) + "..." : text);
                        String time = m.get("timestamp") != null ? String.valueOf(m.get("timestamp")) : "";
                        options.add(new ResourceOption(id, label, "Comment " + time + " - ID: " + id));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.debug("[instagram] graph.instagram.com comments failed: {}", e.getMessage());
        }

        // Strategy 2: graph.facebook.com/{version}/{mediaId}/comments
        try {
            String url = GRAPH_FB_BASE + "/" + version + "/" + mediaId + "/comments?fields=" + enc("id,text,username,timestamp,from,like_count") + "&limit=50&access_token=" + enc(token);
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.get("data") instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        String id = String.valueOf(m.get("id"));
                        String text = m.get("text") != null ? String.valueOf(m.get("text")) : id;
                        String username = m.get("username") != null ? "@" + m.get("username") : "";
                        String label = username + (!username.isBlank() ? ": " : "") + (text.length() > 50 ? text.substring(0, 47) + "..." : text);
                        String time = m.get("timestamp") != null ? String.valueOf(m.get("timestamp")) : "";
                        options.add(new ResourceOption(id, label, "Comment " + time + " - ID: " + id));
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[instagram] Failed to list comments for media {}: {}", mediaId, e.getMessage());
        }

        return options;
    }

    private List<ResourceOption> listConversations(String token, String version, String igUserId) {
        Map<String, ResourceOption> discoveredRecipients = new LinkedHashMap<>();
        String effectiveIgId = (igUserId != null && !igUserId.isBlank()) ? igUserId : "me";
        boolean isIgToken = token != null && token.startsWith("IGA");

        // Resolve my own numeric IG ID so we never include our own account in recipient list
        String resolvedMyId = null;
        try {
            Map<String, Object> me = getJson(GRAPH_IG_BASE + "/me?fields=" + enc("id") + "&access_token=" + enc(token));
            if (me != null && me.containsKey("id")) {
                resolvedMyId = String.valueOf(me.get("id"));
            }
        } catch (Exception ignored) {}
        if (resolvedMyId == null && igUserId != null && !igUserId.isBlank() && !"me".equals(igUserId)) {
            resolvedMyId = igUserId;
        }

        // Primary lightweight field query (excludes heavy nested messages payload to prevent Meta error code 1: "Please reduce the amount of data")
        String fields = "id,updated_time,participants";

        // Strategy 1: graph.instagram.com/me/conversations
        try {
            String url = GRAPH_IG_BASE + "/me/conversations?fields=" + enc(fields) + "&limit=25&access_token=" + enc(token);
            logger.info("[instagram] Strategy 1: GET graph.instagram.com/me/conversations");
            Map<String, Object> resp = getJson(url);
            if (resp != null && resp.get("data") instanceof List<?> convList && !convList.isEmpty()) {
                logger.info("[instagram] Found {} conversations on graph.instagram.com/me", convList.size());
                extractRecipientsFromConversations(convList, resolvedMyId, discoveredRecipients);
            }
        } catch (Exception e) {
            logger.warn("[instagram] Strategy 1 conversations failed: {}", e.getMessage());
        }

        // Strategy 2: graph.instagram.com/{effectiveIgId}/conversations
        if (discoveredRecipients.isEmpty() && !"me".equals(effectiveIgId)) {
            try {
                String url = GRAPH_IG_BASE + "/" + effectiveIgId + "/conversations?fields=" + enc(fields) + "&limit=25&access_token=" + enc(token);
                logger.info("[instagram] Strategy 2: GET graph.instagram.com/{}/conversations", effectiveIgId);
                Map<String, Object> resp = getJson(url);
                if (resp != null && resp.get("data") instanceof List<?> convList && !convList.isEmpty()) {
                    logger.info("[instagram] Found {} conversations on graph.instagram.com/{}", convList.size(), effectiveIgId);
                    extractRecipientsFromConversations(convList, igUserId, discoveredRecipients);
                }
            } catch (Exception e) {
                logger.warn("[instagram] Strategy 2 conversations failed: {}", e.getMessage());
            }
        }

        // Strategy 3: graph.instagram.com fallback with minimal fields (id,participants)
        if (discoveredRecipients.isEmpty()) {
            try {
                String url = GRAPH_IG_BASE + "/me/conversations?fields=" + enc("id,participants") + "&limit=15&access_token=" + enc(token);
                logger.info("[instagram] Strategy 3: GET graph.instagram.com/me/conversations (minimal fields)");
                Map<String, Object> resp = getJson(url);
                if (resp != null && resp.get("data") instanceof List<?> convList && !convList.isEmpty()) {
                    extractRecipientsFromConversations(convList, igUserId, discoveredRecipients);
                }
            } catch (Exception e) {
                logger.debug("[instagram] Strategy 3 minimal conversations failed: {}", e.getMessage());
            }
        }

        // Strategy 4 & 5: Only query graph.facebook.com if using a Facebook User token (not an Instagram Login token IGA...)
        if (discoveredRecipients.isEmpty() && !isIgToken) {
            try {
                String url = GRAPH_FB_BASE + "/" + version + "/" + effectiveIgId + "/conversations?platform=instagram&fields=" + enc(fields) + "&limit=25&access_token=" + enc(token);
                logger.info("[instagram] Strategy 4: GET graph.facebook.com/{}/{}/conversations?platform=instagram", version, effectiveIgId);
                Map<String, Object> resp = getJson(url);
                if (resp != null && resp.get("data") instanceof List<?> convList && !convList.isEmpty()) {
                    logger.info("[instagram] Found {} conversations on graph.facebook.com", convList.size());
                    extractRecipientsFromConversations(convList, igUserId, discoveredRecipients);
                }
            } catch (Exception e) {
                logger.warn("[instagram] Strategy 4 conversations failed: {}", e.getMessage());
            }

            if (discoveredRecipients.isEmpty()) {
                try {
                    String url = GRAPH_FB_BASE + "/" + version + "/me/conversations?platform=instagram&fields=" + enc(fields) + "&limit=25&access_token=" + enc(token);
                    logger.info("[instagram] Strategy 5: GET graph.facebook.com/{}/me/conversations?platform=instagram", version);
                    Map<String, Object> resp = getJson(url);
                    if (resp != null && resp.get("data") instanceof List<?> convList && !convList.isEmpty()) {
                        extractRecipientsFromConversations(convList, igUserId, discoveredRecipients);
                    }
                } catch (Exception e) {
                    logger.warn("[instagram] Strategy 5 conversations failed: {}", e.getMessage());
                }
            }
        }

        if (discoveredRecipients.isEmpty()) {
            logger.info("[instagram] No recent DM conversations found.");
        } else {
            logger.info("[instagram] Discovered {} recent DM contact(s)", discoveredRecipients.size());
        }

        return new ArrayList<>(discoveredRecipients.values());
    }

    private void extractRecipientsFromConversations(List<?> convList, String myIgUserId, Map<String, ResourceOption> out) {
        for (Object item : convList) {
            if (!(item instanceof Map<?, ?> conv)) continue;

            // Extract participants
            List<?> pList = null;
            Object participantsObj = conv.get("participants");
            if (participantsObj instanceof Map<?, ?> pMap && pMap.get("data") instanceof List<?> list) {
                pList = list;
            } else if (participantsObj instanceof List<?> list) {
                pList = list;
            }

            if (pList != null) {
                for (Object pObj : pList) {
                    if (pObj instanceof Map<?, ?> p) {
                        String pId = String.valueOf(p.get("id"));
                        // Skip if it's our own account ID
                        if (myIgUserId != null && myIgUserId.equals(pId)) continue;

                        String name = p.get("name") != null ? String.valueOf(p.get("name")) : null;
                        String username = p.get("username") != null ? String.valueOf(p.get("username")) : null;

                        String displayName = name != null
                                ? (username != null ? name + " (@" + username + ")" : name)
                                : (username != null ? "@" + username : "User " + pId);

                        String desc = (username != null ? "@" + username + " • " : "") + "IGSID: " + pId;
                        out.putIfAbsent(pId, new ResourceOption(pId, displayName, desc));
                    }
                }
            }
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
            logger.warn("[instagram] HTTP request failed: {} -> {}", url, e.getMessage());
            return Map.of();
        }
    }

    private String extractToken(Map<String, Object> credentials) {
        if (credentials == null) return null;
        Object token = credentials.get("accessToken");
        return token != null ? token.toString().trim() : null;
    }
}


