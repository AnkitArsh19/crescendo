package com.crescendo.apps.instagram;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

class IgBase {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IgBase.class);
    private static final RestClient restClient;

    static {
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        restClient = RestClient.builder()
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    static RestClient client() {
        return restClient;
    }

    static String getVersion(ActionContext x) {
        String v = SimpleApiSupport.cred(x, "graphVersion");
        return (v == null || v.isBlank()) ? "v26.0" : (v.startsWith("v") ? v : "v" + v);
    }

    static String extractUrl(ActionContext x, String key) {
        Object val = x.configuration().get(key);
        if (val == null) return "";
        if (val instanceof Map<?, ?> m) {
            Object url = m.get("url");
            if (url != null) return url.toString().trim();
        }
        return val.toString().trim();
    }

    static Map<String, Object> postJson(String url, Map<String, Object> body, ObjectMapper mapper) {
        try {
            String raw = restClient.post()
                    .uri(java.net.URI.create(url))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            if (raw != null && !raw.isBlank()) {
                return mapper.readValue(raw, Map.class);
            }
        } catch (org.springframework.web.client.RestClientResponseException e) {
            String errBody = e.getResponseBodyAsString();
            logger.warn("[instagram-api] HTTP {} POST {} -> {}", e.getStatusCode(), url, errBody);
            try {
                if (errBody != null && !errBody.isBlank()) {
                    return mapper.readValue(errBody, Map.class);
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            logger.warn("[instagram-api] POST {} failed: {}", url, e.getMessage());
        }
        return null;
    }

    static Map<String, Object> getJson(String url, ObjectMapper mapper) {
        try {
            String raw = restClient.get()
                    .uri(java.net.URI.create(url))
                    .retrieve()
                    .body(String.class);
            if (raw != null && !raw.isBlank()) {
                return mapper.readValue(raw, Map.class);
            }
        } catch (org.springframework.web.client.RestClientResponseException e) {
            String errBody = e.getResponseBodyAsString();
            logger.warn("[instagram-api] HTTP {} GET {} -> {}", e.getStatusCode(), url, errBody);
            try {
                if (errBody != null && !errBody.isBlank()) {
                    return mapper.readValue(errBody, Map.class);
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            logger.warn("[instagram-api] GET {} failed: {}", url, e.getMessage());
        }
        return null;
    }
}

@ActionMapping(appKey = "instagram", actionKey = "publish-photo-post")
class InstagramPublishPhotoPostHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramPublishPhotoPostHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            String igUserId = SimpleApiSupport.cfg(c, "igUserId");
            String ver = IgBase.getVersion(c);

            // Step 1: Create media container
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("image_url", IgBase.extractUrl(c, "imageUrl"));
            String caption = SimpleApiSupport.cfg(c, "caption");
            if (!caption.isBlank()) {
                body.put("caption", caption);
            }
            body.put("access_token", token);

            Map<String, Object> containerResp = IgBase.client().post()
                    .uri("https://graph.facebook.com/" + ver + "/{igUserId}/media", igUserId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (containerResp == null || !containerResp.containsKey("id")) {
                return ActionResult.failure("Failed to create Instagram photo container: " + containerResp);
            }

            String creationId = String.valueOf(containerResp.get("id"));

            // Step 2: Publish media container
            Map<String, Object> publishResp = IgBase.client().post()
                    .uri("https://graph.facebook.com/" + ver + "/{igUserId}/media_publish", igUserId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("creation_id", creationId, "access_token", token))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(publishResp != null ? publishResp : Map.of("id", creationId, "status", "PUBLISHED"));
        } catch (Exception e) {
            return ActionResult.failure("Instagram photo post failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "instagram", actionKey = "publish-reel-post")
class InstagramPublishReelPostHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramPublishReelPostHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            String igUserId = SimpleApiSupport.cfg(c, "igUserId");
            String ver = IgBase.getVersion(c);

            // Step 1: Create video/reel container
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("media_type", "REELS");
            body.put("video_url", IgBase.extractUrl(c, "videoUrl"));
            String caption = SimpleApiSupport.cfg(c, "caption");
            if (!caption.isBlank()) {
                body.put("caption", caption);
            }
            String coverUrl = IgBase.extractUrl(c, "coverUrl");
            if (!coverUrl.isBlank()) {
                body.put("cover_url", coverUrl);
            }
            body.put("access_token", token);

            Map<String, Object> containerResp = IgBase.client().post()
                    .uri("https://graph.facebook.com/" + ver + "/{igUserId}/media", igUserId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (containerResp == null || !containerResp.containsKey("id")) {
                return ActionResult.failure("Failed to create Instagram Reel container: " + containerResp);
            }

            String creationId = String.valueOf(containerResp.get("id"));

            // Wait a brief moment for container processing
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

            // Step 2: Publish media container
            Map<String, Object> publishResp = IgBase.client().post()
                    .uri("https://graph.facebook.com/" + ver + "/{igUserId}/media_publish", igUserId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("creation_id", creationId, "access_token", token))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(publishResp != null ? publishResp : Map.of("id", creationId, "status", "PUBLISHED"));
        } catch (Exception e) {
            return ActionResult.failure("Instagram Reel publishing failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "instagram", actionKey = "send-direct-message")
class InstagramSendDirectMessageHandler implements ActionHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InstagramSendDirectMessageHandler.class);
    private final ObjectMapper m;

    InstagramSendDirectMessageHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            String igUserId = SimpleApiSupport.cfg(c, "igUserId");
            String recipientId = SimpleApiSupport.cfg(c, "recipientId");
            String message = SimpleApiSupport.cfg(c, "message");
            String ver = IgBase.getVersion(c);
            String target = (igUserId != null && !igUserId.isBlank()) ? igUserId : "me";

            if (recipientId == null || recipientId.isBlank()) {
                return ActionResult.failure("Recipient ID is required to send an Instagram Direct Message.");
            }
            if (message == null || message.isBlank()) {
                return ActionResult.failure("Message text cannot be empty.");
            }

            // Prepare payload
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("recipient", Map.of("id", recipientId));
            body.put("message", Map.of("text", message));

            String encodedToken = java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);

            // Candidate endpoints
            List<String> endpoints = new ArrayList<>();
            endpoints.add("https://graph.instagram.com/me/messages");
            if (!"me".equals(target)) {
                endpoints.add("https://graph.instagram.com/" + target + "/messages");
            }
            endpoints.add("https://graph.instagram.com/v22.0/me/messages");
            if (token != null && !token.startsWith("IGA")) {
                endpoints.add("https://graph.facebook.com/" + ver + "/" + target + "/messages");
                endpoints.add("https://graph.facebook.com/" + ver + "/me/messages");
            }

            Map<String, Object> lastError = null;
            for (String endpoint : endpoints) {
                logger.info("[instagram-dm] Sending DM to endpoint: {} for recipient: {}", endpoint, recipientId);
                String url = endpoint + "?access_token=" + encodedToken;
                Map<String, Object> resp = IgBase.postJson(url, body, m);
                if (isSuccess(resp)) {
                    logger.info("[instagram-dm] DM delivered successfully via: {}", endpoint);
                    return ActionResult.success(resp);
                }
                if (resp != null && resp.containsKey("error")) {
                    lastError = resp;
                }
            }

            if (lastError != null && lastError.get("error") instanceof Map<?, ?> errMap) {
                String msg = String.valueOf(errMap.get("message"));
                Object code = errMap.get("code");
                Object subcode = errMap.get("error_subcode");

                if ("2534022".equals(String.valueOf(subcode)) || "10".equals(String.valueOf(code)) || msg.toLowerCase().contains("outside of allowed window")) {
                    return ActionResult.failure(
                            "Instagram DM blocked by Meta: The 24-hour messaging window is closed for recipient " + recipientId +
                            ". Under Meta's Platform Policy, automated API messages can only be sent within 24 hours of the user's latest incoming message to your account. " +
                            "To send messages to this contact: have them send a new direct message to your Instagram account in the Instagram mobile app, then re-test."
                    );
                }
                return ActionResult.failure("Instagram send DM failed: " + msg + " (Code: " + code + (subcode != null ? ", Subcode: " + subcode : "") + ")");
            }

            return ActionResult.failure("Instagram send DM failed. Please verify recipient ID and connection.");
        } catch (Exception e) {
            return ActionResult.failure("Instagram send DM failed: " + e.getMessage());
        }
    }

    private boolean isSuccess(Map<String, Object> resp) {
        return resp != null && (resp.containsKey("message_id") || resp.containsKey("recipient_id")) && !resp.containsKey("error");
    }
}

@ActionMapping(appKey = "instagram", actionKey = "reply-comment")
class InstagramReplyCommentHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramReplyCommentHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            String commentId = SimpleApiSupport.cfg(c, "commentId");
            String message = SimpleApiSupport.cfg(c, "message");
            String ver = IgBase.getVersion(c);
            String encodedToken = java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);

            Map<String, Object> body = Map.of("message", message);
            Map<String, Object> resp = null;

            // Strategy 1: graph.instagram.com/{commentId}/replies
            String url1 = "https://graph.instagram.com/" + commentId + "/replies?access_token=" + encodedToken;
            resp = IgBase.postJson(url1, body, m);

            // Strategy 2: graph.facebook.com/{ver}/{commentId}/replies
            if (resp == null || !resp.containsKey("id")) {
                String url2 = "https://graph.facebook.com/" + ver + "/" + commentId + "/replies?access_token=" + encodedToken;
                resp = IgBase.postJson(url2, body, m);
            }

            if (resp != null && resp.containsKey("id")) {
                return ActionResult.success(Map.of(
                        "success", true,
                        "replyId", resp.get("id"),
                        "commentId", commentId,
                        "replyMessage", message
                ));
            }

            return ActionResult.failure("Instagram reply comment failed: " + (resp != null ? resp.get("error") : "Unknown error"));
        } catch (Exception e) {
            return ActionResult.failure("Instagram reply comment failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "instagram", actionKey = "reply-latest-comment")
class InstagramReplyLatestCommentHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramReplyLatestCommentHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            String mediaId = SimpleApiSupport.cfg(c, "mediaId");
            String message = SimpleApiSupport.cfg(c, "message");
            String matchingKeyword = SimpleApiSupport.cfg(c, "matchingKeyword").trim().toLowerCase();
            String ver = IgBase.getVersion(c);
            String encodedToken = java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);

            // Step 1: Fetch recent comments for media
            String queryUrl1 = "https://graph.instagram.com/" + mediaId + "/comments?fields=id,text,timestamp,username,from&limit=50&access_token=" + encodedToken;
            Map<String, Object> commentsResp = IgBase.getJson(queryUrl1, m);

            if (commentsResp == null || !commentsResp.containsKey("data")) {
                String queryUrl2 = "https://graph.facebook.com/" + ver + "/" + mediaId + "/comments?fields=id,text,timestamp,username,from&limit=50&access_token=" + encodedToken;
                commentsResp = IgBase.getJson(queryUrl2, m);
            }

            if (commentsResp == null || !(commentsResp.get("data") instanceof List<?> list) || list.isEmpty()) {
                return ActionResult.success(Map.of(
                        "success", false,
                        "status", "NO_COMMENTS_FOUND",
                        "message", "No comments were found on post: " + mediaId
                ));
            }

            // Step 2: Find target comment (matching keyword if specified, or latest)
            Map<String, Object> targetComment = null;
            for (Object item : list) {
                if (item instanceof Map<?, ?> commentMap) {
                    String commentText = commentMap.get("text") != null ? commentMap.get("text").toString() : "";
                    if (matchingKeyword.isBlank() || commentText.toLowerCase().contains(matchingKeyword)) {
                        targetComment = (Map<String, Object>) commentMap;
                        break;
                    }
                }
            }

            if (targetComment == null) {
                return ActionResult.success(Map.of(
                        "success", false,
                        "status", "NO_KEYWORD_MATCH",
                        "message", "No comment matching keyword '" + matchingKeyword + "' found among recent " + list.size() + " comments on post " + mediaId
                ));
            }

            String targetCommentId = String.valueOf(targetComment.get("id"));
            String targetAuthor = targetComment.get("username") != null ? "@" + targetComment.get("username") : "User";
            String targetText = targetComment.get("text") != null ? targetComment.get("text").toString() : "";

            // Step 3: Post the reply
            Map<String, Object> replyBody = Map.of("message", message);
            String replyUrl1 = "https://graph.instagram.com/" + targetCommentId + "/replies?access_token=" + encodedToken;
            Map<String, Object> replyResp = IgBase.postJson(replyUrl1, replyBody, m);

            if (replyResp == null || !replyResp.containsKey("id")) {
                String replyUrl2 = "https://graph.facebook.com/" + ver + "/" + targetCommentId + "/replies?access_token=" + encodedToken;
                replyResp = IgBase.postJson(replyUrl2, replyBody, m);
            }

            String replyId = replyResp != null && replyResp.containsKey("id") ? String.valueOf(replyResp.get("id")) : "UNKNOWN";

            return ActionResult.success(Map.of(
                    "success", true,
                    "replyId", replyId,
                    "repliedToCommentId", targetCommentId,
                    "commentAuthor", targetAuthor,
                    "commentText", targetText,
                    "replyMessage", message,
                    "matchedKeyword", matchingKeyword.isBlank() ? "(latest comment)" : matchingKeyword
            ));
        } catch (Exception e) {
            return ActionResult.failure("Instagram reply to latest comment failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "instagram", actionKey = "get-media-comments")
class InstagramGetMediaCommentsHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramGetMediaCommentsHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            String mediaId = SimpleApiSupport.cfg(c, "mediaId");
            String limit = SimpleApiSupport.cfg(c, "limit");
            if (limit.isBlank()) limit = "25";
            String ver = IgBase.getVersion(c);
            String encodedToken = java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);

            String url1 = "https://graph.instagram.com/" + mediaId + "/comments?fields=id,text,timestamp,username,from,like_count&limit=" + limit + "&access_token=" + encodedToken;
            Map<String, Object> resp = IgBase.getJson(url1, m);

            if (resp == null || !resp.containsKey("data")) {
                String url2 = "https://graph.facebook.com/" + ver + "/" + mediaId + "/comments?fields=id,text,timestamp,username,from,like_count&limit=" + limit + "&access_token=" + encodedToken;
                resp = IgBase.getJson(url2, m);
            }

            return ActionResult.success(resp != null ? resp : Map.of());
        } catch (Exception e) {
            return ActionResult.failure("Instagram get media comments failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "instagram", actionKey = "get-user-media")
class InstagramGetUserMediaHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramGetUserMediaHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            String igUserId = SimpleApiSupport.cfg(c, "igUserId");
            String limit = SimpleApiSupport.cfg(c, "limit");
            if (limit.isBlank()) limit = "25";
            String ver = IgBase.getVersion(c);

            Map<String, Object> resp = IgBase.client().get()
                    .uri("https://graph.facebook.com/" + ver + "/{igUserId}/media?fields=id,caption,media_type,media_url,permalink,thumbnail_url,timestamp,like_count,comments_count&limit={limit}", igUserId, limit)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(resp != null ? resp : Map.of());
        } catch (Exception e) {
            return ActionResult.failure("Instagram get user media failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "instagram", actionKey = "get-media-insights")
class InstagramGetMediaInsightsHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramGetMediaInsightsHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            String mediaId = SimpleApiSupport.cfg(c, "mediaId");
            String ver = IgBase.getVersion(c);

            Map<String, Object> resp = IgBase.client().get()
                    .uri("https://graph.facebook.com/" + ver + "/{mediaId}/insights?metric=impressions,reach,total_interactions,likes,comments,saved,shares", mediaId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(resp != null ? resp : Map.of());
        } catch (Exception e) {
            return ActionResult.failure("Instagram get media insights failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "instagram", actionKey = "get-account-info")
class InstagramGetAccountInfoHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramGetAccountInfoHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            String igUserId = SimpleApiSupport.cfg(c, "igUserId");
            String ver = IgBase.getVersion(c);

            Map<String, Object> resp = IgBase.client().get()
                    .uri("https://graph.facebook.com/" + ver + "/{igUserId}?fields=id,username,name,account_type,profile_picture_url,followers_count,follows_count,media_count", igUserId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(resp != null ? resp : Map.of());
        } catch (Exception e) {
            return ActionResult.failure("Instagram get account info failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "instagram", actionKey = "create-media-container")
class InstagramCreateMediaContainerHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramCreateMediaContainerHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            String ver = IgBase.getVersion(c);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("image_url", IgBase.extractUrl(c, "imageUrl"));
            String caption = SimpleApiSupport.cfg(c, "caption");
            if (!caption.isBlank()) {
                body.put("caption", caption);
            }
            body.put("access_token", token);

            Map<String, Object> res = IgBase.client().post()
                    .uri("https://graph.facebook.com/" + ver + "/{igUserId}/media", SimpleApiSupport.cfg(c, "igUserId"))
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(res != null ? res : Map.of());
        } catch (Exception e) {
            return ActionResult.failure("Instagram create media container failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "instagram", actionKey = "publish-media")
class InstagramPublishMediaHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramPublishMediaHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            String ver = IgBase.getVersion(c);
            Map<String, Object> res = IgBase.client().post()
                    .uri("https://graph.facebook.com/" + ver + "/{igUserId}/media_publish", SimpleApiSupport.cfg(c, "igUserId"))
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "creation_id", SimpleApiSupport.cfg(c, "creationId"),
                            "access_token", token
                    ))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(res != null ? res : Map.of());
        } catch (Exception e) {
            return ActionResult.failure("Instagram publish media failed: " + e.getMessage());
        }
    }
}
