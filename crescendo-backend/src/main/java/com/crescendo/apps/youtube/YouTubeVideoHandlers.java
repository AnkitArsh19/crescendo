package com.crescendo.apps.youtube;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
// import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
// import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;

/**
 * Grouped handler for YouTube Video operations.
 *
 * <p>Operations:
 * <ul>
 *   <li>{@code get}    — videos.list (id)</li>
 *   <li>{@code getAll} — videos.list</li>
 *   <li>{@code rate}   — videos.rate</li>
 *   <li>{@code upload} — videos.insert (multipart)</li>
 *   <li>{@code update} — videos.update</li>
 * </ul>
 */
@Component
public class YouTubeVideoHandlers {

// private static final Logger logger = LoggerFactory.getLogger(YouTubeVideoHandlers.class);
    private static final String BASE = "https://www.googleapis.com/youtube/v3/videos";
    private final ObjectMapper mapper = new ObjectMapper();

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "youtube", actionKey = "getVideo")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String videoId = YouTubeSupport.require(context.configuration(), "videoId");
        if (videoId == null) return ActionResult.failure("'videoId' is required");

        try {
            String uri = BASE + "?part=snippet,contentDetails,statistics&id=" + videoId;
            String apiKey = YouTubeSupport.resolveApiKey(context);
            if (apiKey != null) uri += "&key=" + apiKey;

            Map<String, Object> response = YouTubeSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("YouTube getVideo failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "youtube", actionKey = "getAllVideos")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String chart = YouTubeSupport.opt(config, "chart", null);
        String myRating = YouTubeSupport.opt(config, "myRating", null);
        int maxResults = YouTubeSupport.parseIntOpt(config, "maxResults", 50);

        try {
            StringBuilder uri = new StringBuilder(BASE + "?part=snippet,contentDetails,statistics&maxResults=" + maxResults);
            if (chart != null) uri.append("&chart=").append(chart);
            if (myRating != null) uri.append("&myRating=").append(myRating);

            String apiKey = YouTubeSupport.resolveApiKey(context);
            if (apiKey != null) uri.append("&key=").append(apiKey);

            Map<String, Object> response = YouTubeSupport.clientBuilder(context).build().get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("YouTube getAllVideos failed: " + e.getMessage());
        }
    }

    // ── rate ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "youtube", actionKey = "rateVideo")
    public ActionResult rate(ActionContext context) {
        String videoId = YouTubeSupport.require(context.configuration(), "videoId");
        String rating = YouTubeSupport.require(context.configuration(), "rating"); // like, dislike, none
        if (videoId == null || rating == null) return ActionResult.failure("'videoId' and 'rating' are required");

        if (YouTubeSupport.resolveToken(context) == null) {
            return ActionResult.failure("YouTube rateVideo requires an OAuth accessToken");
        }

        try {
            YouTubeSupport.clientBuilder(context).build().post()
                    .uri(BASE + "/rate?id=" + videoId + "&rating=" + rating)
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "videoId", videoId, "rating", rating));
        } catch (Exception e) {
            return ActionResult.failure("YouTube rateVideo failed: " + e.getMessage());
        }
    }

    // ── upload ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "youtube", actionKey = "uploadVideo")
    public ActionResult upload(ActionContext c) {
        try {
            String token = YouTubeSupport.resolveToken(c);
            if (token == null || token.isBlank()) {
                return ActionResult.failure("YouTube upload requires an OAuth access token");
            }
            Map<String, Object> config = c.configuration();
            byte[] video = Base64.getDecoder().decode(YouTubeSupport.require(config, "videoBase64"));
            String mimeType = YouTubeSupport.opt(config, "mimeType", "video/mp4");
            String boundary = "crescendo-" + UUID.randomUUID();
            byte[] body = multipart(boundary, metadata(config, false), video, mimeType);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=multipart&part=snippet,status"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "multipart/related; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return parsedHttp(response, "YouTube upload failed");
        } catch (Exception e) {
            return ActionResult.failure("YouTube upload failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "youtube", actionKey = "updateVideo")
    public ActionResult update(ActionContext c) {
        try {
            String token = YouTubeSupport.resolveToken(c);
            if (token == null || token.isBlank()) {
                return ActionResult.failure("YouTube update requires an OAuth access token");
            }
            Map<String, Object> config = c.configuration();
            Map<String, Object> body = metadata(config, true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/youtube/v3/videos?part=snippet,status"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return parsedHttp(response, "YouTube update failed");
        } catch (Exception e) {
            return ActionResult.failure("YouTube update failed: " + e.getMessage());
        }
    }

    private byte[] multipart(String boundary, Map<String, Object> metadata, byte[] video, String mimeType) throws Exception {
        String head = "--" + boundary + "\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                + mapper.writeValueAsString(metadata) + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: " + mimeType + "\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";
        byte[] h = head.getBytes(StandardCharsets.UTF_8);
        byte[] t = tail.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[h.length + video.length + t.length];
        System.arraycopy(h, 0, out, 0, h.length);
        System.arraycopy(video, 0, out, h.length, video.length);
        System.arraycopy(t, 0, out, h.length + video.length, t.length);
        return out;
    }

    private ActionResult parsedHttp(HttpResponse<String> response, String failurePrefix) throws Exception {
        Object data = response.body() != null && !response.body().isBlank()
                ? mapper.readValue(response.body(), Object.class)
                : Map.of();
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return ActionResult.success(Map.of("status", response.statusCode(), "data", data, "raw", response.body()));
        }
        return ActionResult.failure(failurePrefix + " (" + response.statusCode() + "): " + response.body());
    }

    private Map<String, Object> metadata(Map<String, Object> config, boolean includeId) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (includeId) {
            root.put("id", YouTubeSupport.require(config, "videoId"));
        }
        Map<String, Object> snippet = new LinkedHashMap<>();
        snippet.put("title", YouTubeSupport.require(config, "title"));
        snippet.put("description", YouTubeSupport.opt(config, "description", ""));
        snippet.put("categoryId", YouTubeSupport.opt(config, "categoryId", "22"));
        
        List<String> tags = tags(YouTubeSupport.opt(config, "tags", ""));
        if (!tags.isEmpty()) {
            snippet.put("tags", tags);
        }
        root.put("snippet", snippet);
        root.put("status", Map.of("privacyStatus", YouTubeSupport.opt(config, "privacyStatus", "private")));
        return root;
    }

    private List<String> tags(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String tag : csv.split(",")) {
            if (!tag.trim().isBlank()) out.add(tag.trim());
        }
        return out;
    }
}
