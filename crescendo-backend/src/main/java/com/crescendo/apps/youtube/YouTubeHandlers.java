package com.crescendo.apps.youtube;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

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

@ActionMapping(appKey = "youtube", actionKey = "search")
class YouTubeSearchHandler implements ActionHandler {
    private final ObjectMapper mapper;
    YouTubeSearchHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String key = SimpleApiSupport.cred(c, "apiKey");
            RestClient client = key.isBlank()
                    ? SimpleApiSupport.bearer("https://www.googleapis.com/youtube/v3", SimpleApiSupport.cred(c, "accessToken"))
                    : RestClient.create("https://www.googleapis.com/youtube/v3");
            String uri = key.isBlank()
                    ? "/search?part=snippet&type=video&q={q}&maxResults={max}"
                    : "/search?part=snippet&type=video&q={q}&maxResults={max}&key=" + key;
            return SimpleApiSupport.parsed(mapper, client.get().uri(uri, SimpleApiSupport.cfg(c, "query"),
                    Math.max(1, SimpleApiSupport.intCfg(c, "maxResults", 10))).retrieve().body(String.class));
        } catch (Exception e) {
            return ActionResult.failure("YouTube search failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "youtube", actionKey = "list-my-videos")
class YouTubeListMyVideosHandler implements ActionHandler {
    private final ObjectMapper mapper;
    YouTubeListMyVideosHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            var client = SimpleApiSupport.bearer("https://www.googleapis.com/youtube/v3", SimpleApiSupport.cred(c, "accessToken"));
            String ch = client.get().uri("/channels?part=contentDetails&mine=true").retrieve().body(String.class);
            Object data = mapper.readValue(ch, Object.class);
            String uploads = String.valueOf(((Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) ((List<?>) ((Map<?, ?>) data).get("items")).get(0))
                    .get("contentDetails")).get("relatedPlaylists")).get("uploads"));
            return SimpleApiSupport.parsed(mapper, client.get().uri("/playlistItems?part=snippet&playlistId={id}&maxResults={max}",
                    uploads, Math.max(1, SimpleApiSupport.intCfg(c, "maxResults", 10))).retrieve().body(String.class));
        } catch (Exception e) {
            return ActionResult.failure("YouTube list my videos failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "youtube", actionKey = "upload-video")
class YouTubeUploadVideoHandler implements ActionHandler {
    private final ObjectMapper mapper;
    YouTubeUploadVideoHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            if (token.isBlank()) {
                return ActionResult.failure("YouTube upload requires an OAuth access token with youtube.upload scope");
            }
            byte[] video = Base64.getDecoder().decode(SimpleApiSupport.cfg(c, "videoBase64"));
            String mimeType = blank(SimpleApiSupport.cfg(c, "mimeType")) ? "video/mp4" : SimpleApiSupport.cfg(c, "mimeType");
            String boundary = "crescendo-" + UUID.randomUUID();
            byte[] body = multipart(boundary, metadata(c, false), video, mimeType);
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

    private Map<String, Object> metadata(ActionContext c, boolean includeId) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (includeId) {
            root.put("id", SimpleApiSupport.cfg(c, "videoId"));
        }
        Map<String, Object> snippet = new LinkedHashMap<>();
        snippet.put("title", SimpleApiSupport.cfg(c, "title"));
        snippet.put("description", SimpleApiSupport.cfg(c, "description"));
        snippet.put("categoryId", blank(SimpleApiSupport.cfg(c, "categoryId")) ? "22" : SimpleApiSupport.cfg(c, "categoryId"));
        List<String> tags = tags(SimpleApiSupport.cfg(c, "tags"));
        if (!tags.isEmpty()) {
            snippet.put("tags", tags);
        }
        root.put("snippet", snippet);
        root.put("status", Map.of("privacyStatus", blank(SimpleApiSupport.cfg(c, "privacyStatus")) ? "private" : SimpleApiSupport.cfg(c, "privacyStatus")));
        return root;
    }

    private List<String> tags(String csv) {
        if (blank(csv)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String tag : csv.split(",")) {
            if (!tag.trim().isBlank()) {
                out.add(tag.trim());
            }
        }
        return out;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}

@ActionMapping(appKey = "youtube", actionKey = "update-video")
class YouTubeUpdateVideoHandler extends YouTubeUploadVideoHandler {
    private final ObjectMapper mapper;
    YouTubeUpdateVideoHandler(ObjectMapper mapper) {
        super(mapper);
        this.mapper = mapper;
    }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String token = SimpleApiSupport.cred(c, "accessToken");
            if (token.isBlank()) {
                return ActionResult.failure("YouTube update requires an OAuth access token");
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", SimpleApiSupport.cfg(c, "videoId"));
            Map<String, Object> snippet = new LinkedHashMap<>();
            snippet.put("title", SimpleApiSupport.cfg(c, "title"));
            snippet.put("description", SimpleApiSupport.cfg(c, "description"));
            snippet.put("categoryId", SimpleApiSupport.cfg(c, "categoryId").isBlank() ? "22" : SimpleApiSupport.cfg(c, "categoryId"));
            List<String> tags = new ArrayList<>();
            if (!SimpleApiSupport.cfg(c, "tags").isBlank()) {
                for (String tag : SimpleApiSupport.cfg(c, "tags").split(",")) {
                    if (!tag.trim().isBlank()) {
                        tags.add(tag.trim());
                    }
                }
                snippet.put("tags", tags);
            }
            body.put("snippet", snippet);
            body.put("status", Map.of("privacyStatus", SimpleApiSupport.cfg(c, "privacyStatus").isBlank() ? "private" : SimpleApiSupport.cfg(c, "privacyStatus")));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/youtube/v3/videos?part=snippet,status"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            Object data = response.body() != null && !response.body().isBlank()
                    ? mapper.readValue(response.body(), Object.class)
                    : Map.of();
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return ActionResult.success(Map.of("status", response.statusCode(), "data", data, "raw", response.body()));
            }
            return ActionResult.failure("YouTube update failed (" + response.statusCode() + "): " + response.body());
        } catch (Exception e) {
            return ActionResult.failure("YouTube update failed: " + e.getMessage());
        }
    }
}
