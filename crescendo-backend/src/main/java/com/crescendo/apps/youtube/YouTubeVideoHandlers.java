package com.crescendo.apps.youtube;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.storage.MediaStreamResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import tools.jackson.databind.ObjectMapper;

/**
 * Grouped handler for YouTube Video operations.
 *
 * <p>Operations:
 * <ul>
 *   <li>{@code get}    — videos.list (id)</li>
 *   <li>{@code getAll} — videos.list</li>
 *   <li>{@code rate}   — videos.rate</li>
 *   <li>{@code upload} — videos.insert (resumable chunked streaming)</li>
 *   <li>{@code update} — videos.update</li>
 * </ul>
 */
@Component
public class YouTubeVideoHandlers {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeVideoHandlers.class);
    private static final String BASE = "https://www.googleapis.com/youtube/v3/videos";
    private static final int CHUNK_SIZE = 5 * 1024 * 1024; // 5MB chunks (standard Google Resumable Upload chunk size)

    private final ObjectMapper mapper = new ObjectMapper();
    private final MediaStreamResolver mediaStreamResolver;

    public YouTubeVideoHandlers() {
        this(new MediaStreamResolver());
    }

    @Autowired
    public YouTubeVideoHandlers(MediaStreamResolver mediaStreamResolver) {
        this.mediaStreamResolver = mediaStreamResolver;
    }

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

    // ── upload (Resumable Chunked Stream) ─────────────────────────────────────
    @ActionMapping(appKey = "youtube", actionKey = "uploadVideo")
    public ActionResult upload(ActionContext c) {
        try {
            String token = YouTubeSupport.resolveToken(c);
            if (token == null || token.isBlank()) {
                return ActionResult.failure("YouTube upload requires an OAuth access token");
            }

            Map<String, Object> config = c.configuration();
            String title = YouTubeSupport.require(config, "title");
            if (title == null) {
                return ActionResult.failure("'title' is required");
            }

            Object rawVideo = config.get("videoSource");
            if (rawVideo == null) rawVideo = config.get("videoUrl");
            if (rawVideo == null) rawVideo = config.get("videoBase64");
            if (rawVideo == null) rawVideo = config.get("file");

            if (rawVideo == null) {
                return ActionResult.failure("Video source is required (provide a video file, Google Drive link, or direct video URL)");
            }

            String customMimeType = YouTubeSupport.opt(config, "mimeType", "video/mp4");
            HttpClient httpClient = HttpClient.newHttpClient();

            try (MediaStreamResolver.MediaSource media = mediaStreamResolver.resolve(rawVideo, customMimeType)) {
                String mimeType = media.contentType() != null ? media.contentType() : customMimeType;
                long totalLength = media.contentLength();

                // Step 1: Initiate YouTube Resumable Upload Session
                HttpRequest.Builder initReqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .header("X-Upload-Content-Type", mimeType);

                if (totalLength > 0) {
                    initReqBuilder.header("X-Upload-Content-Length", String.valueOf(totalLength));
                }

                Map<String, Object> metadata = metadata(config, false);
                HttpRequest initRequest = initReqBuilder
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(metadata)))
                        .build();

                HttpResponse<String> initResponse = httpClient.send(initRequest, HttpResponse.BodyHandlers.ofString());
                if (initResponse.statusCode() != 200) {
                    return ActionResult.failure("Failed to initialize YouTube resumable upload session (" + initResponse.statusCode() + "): " + initResponse.body());
                }

                Optional<String> locationOpt = initResponse.headers().firstValue("Location");
                if (locationOpt.isEmpty()) {
                    return ActionResult.failure("YouTube upload initialization did not return a resumable session URL");
                }
                String uploadUrl = locationOpt.get();
                logger.info("[YouTube] Resumable upload session initiated: totalLength={}", totalLength);

                // Step 2: Stream Video Chunks with Network Resilience & Status Resumption
                InputStream in = media.stream();
                byte[] buffer = new byte[CHUNK_SIZE];
                long bytesUploaded = 0;
                HttpResponse<String> finalResponse = null;

                while (true) {
                    int bytesRead = readFully(in, buffer);
                    if (bytesRead <= 0) break;

                    long chunkStart = bytesUploaded;
                    long chunkEnd = bytesUploaded + bytesRead - 1;
                    String totalStr = totalLength > 0 ? String.valueOf(totalLength) : "*";

                    byte[] chunkData = Arrays.copyOf(buffer, bytesRead);
                    boolean isLastChunk = (totalLength > 0 && chunkEnd + 1 == totalLength) || (bytesRead < CHUNK_SIZE && totalLength <= 0);

                    HttpRequest chunkRequest = HttpRequest.newBuilder()
                            .uri(URI.create(uploadUrl))
                            .header("Content-Type", mimeType)
                            .header("Content-Range", "bytes " + chunkStart + "-" + chunkEnd + "/" + (isLastChunk ? (chunkEnd + 1) : totalStr))
                            .PUT(HttpRequest.BodyPublishers.ofByteArray(chunkData))
                            .build();

                    HttpResponse<String> chunkResponse = sendWithRetry(httpClient, chunkRequest, uploadUrl, totalLength);
                    int code = chunkResponse.statusCode();

                    if (code == 200 || code == 201) {
                        finalResponse = chunkResponse;
                        break;
                    } else if (code == 308) {
                        // 308 Resume Incomplete: parse Range header to verify byte offset
                        Optional<String> range = chunkResponse.headers().firstValue("Range");
                        if (range.isPresent() && range.get().contains("-")) {
                            String lastByteStr = range.get().substring(range.get().lastIndexOf('-') + 1).trim();
                            try {
                                bytesUploaded = Long.parseLong(lastByteStr) + 1;
                            } catch (NumberFormatException ignored) {
                                bytesUploaded += bytesRead;
                            }
                        } else {
                            bytesUploaded += bytesRead;
                        }
                    } else {
                        return ActionResult.failure("YouTube chunk upload failed (" + code + "): " + chunkResponse.body());
                    }
                }

                if (finalResponse != null) {
                    return parsedHttp(finalResponse, "YouTube video upload finished");
                }
                return ActionResult.failure("YouTube video upload completed without a final response");
            }
        } catch (Exception e) {
            logger.error("[YouTube] Upload failed: {}", e.getMessage(), e);
            return ActionResult.failure("YouTube upload failed: " + e.getMessage());
        }
    }

    private int readFully(InputStream in, byte[] buffer) throws Exception {
        int totalRead = 0;
        while (totalRead < buffer.length) {
            int read = in.read(buffer, totalRead, buffer.length - totalRead);
            if (read == -1) break;
            totalRead += read;
        }
        return totalRead;
    }

    private HttpResponse<String> sendWithRetry(HttpClient client, HttpRequest req, String uploadUrl, long totalLength) throws Exception {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return client.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                logger.warn("[YouTube] Network error on chunk upload (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) throw e;
                Thread.sleep(attempt * 1000L);
            }
        }
        throw new RuntimeException("YouTube upload chunk retry limit reached");
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

    private ActionResult parsedHttp(HttpResponse<String> response, String failurePrefix) throws Exception {
        Object data = response.body() != null && !response.body().isBlank()
                ? mapper.readValue(response.body(), Object.class)
                : Map.of();
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return ActionResult.success(Map.of("status", response.statusCode(), "data", data, "raw", response.body()));
        }
        return ActionResult.failure(failurePrefix + " (" + response.statusCode() + "): " + response.body());
    }

    private static final Map<String, String> CATEGORY_MAP = Map.ofEntries(
            Map.entry("film & animation", "1"),
            Map.entry("film and animation", "1"),
            Map.entry("film", "1"),
            Map.entry("animation", "1"),
            Map.entry("autos & vehicles", "2"),
            Map.entry("autos and vehicles", "2"),
            Map.entry("autos", "2"),
            Map.entry("vehicles", "2"),
            Map.entry("music", "10"),
            Map.entry("pets & animals", "15"),
            Map.entry("pets and animals", "15"),
            Map.entry("pets", "15"),
            Map.entry("animals", "15"),
            Map.entry("sports", "17"),
            Map.entry("travel & events", "19"),
            Map.entry("travel and events", "19"),
            Map.entry("travel", "19"),
            Map.entry("events", "19"),
            Map.entry("gaming", "20"),
            Map.entry("people & blogs", "22"),
            Map.entry("people and blogs", "22"),
            Map.entry("people", "22"),
            Map.entry("blogs", "22"),
            Map.entry("comedy", "23"),
            Map.entry("entertainment", "24"),
            Map.entry("news & politics", "25"),
            Map.entry("news and politics", "25"),
            Map.entry("news", "25"),
            Map.entry("politics", "25"),
            Map.entry("howto & style", "26"),
            Map.entry("howto and style", "26"),
            Map.entry("howto", "26"),
            Map.entry("style", "26"),
            Map.entry("education", "27"),
            Map.entry("science & technology", "28"),
            Map.entry("science and technology", "28"),
            Map.entry("science", "28"),
            Map.entry("technology", "28"),
            Map.entry("nonprofits & activism", "29"),
            Map.entry("nonprofits and activism", "29")
    );

    private String resolveCategoryId(Object rawCategory) {
        if (rawCategory == null) return "22"; // default: People & Blogs
        String val = rawCategory.toString().trim();
        if (val.isEmpty() || "undefined".equalsIgnoreCase(val) || "null".equalsIgnoreCase(val)) {
            return "22";
        }
        // If already numeric ID (e.g. "22", "24", "1", "28")
        if (val.matches("^\\d+$")) {
            return val;
        }
        // Check standard name / label
        String lower = val.toLowerCase();
        if (CATEGORY_MAP.containsKey(lower)) {
            return CATEGORY_MAP.get(lower);
        }
        for (Map.Entry<String, String> e : CATEGORY_MAP.entrySet()) {
            if (lower.contains(e.getKey()) || e.getKey().contains(lower)) {
                return e.getValue();
            }
        }
        return "22"; // Safe fallback
    }

    private Map<String, Object> metadata(Map<String, Object> config, boolean includeId) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (includeId) {
            root.put("id", YouTubeSupport.require(config, "videoId"));
        }
        Map<String, Object> snippet = new LinkedHashMap<>();
        snippet.put("title", YouTubeSupport.require(config, "title"));
        snippet.put("description", YouTubeSupport.opt(config, "description", ""));

        Object rawCat = config.get("categoryId");
        if (rawCat == null) rawCat = config.get("videoCategoryId");
        snippet.put("categoryId", resolveCategoryId(rawCat));

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
