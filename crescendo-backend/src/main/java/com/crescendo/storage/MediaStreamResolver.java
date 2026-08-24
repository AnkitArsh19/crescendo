package com.crescendo.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MediaStreamResolver {

    private static final Logger logger = LoggerFactory.getLogger(MediaStreamResolver.class);
    private static final Pattern GOOGLE_DRIVE_PATTERN = Pattern.compile("https?://drive\\.google\\.com/(?:file/d/|open\\?id=|uc\\?(?:[^&]*&)*id=)([a-zA-Z0-9_-]+)");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FileStorageService fileStorageService;
    private final com.crescendo.storage.security.UrlSecurityValidator urlSecurityValidator;

    public MediaStreamResolver() {
        this.fileStorageService = null;
        this.urlSecurityValidator = new com.crescendo.storage.security.UrlSecurityValidator();
    }

    @Autowired
    public MediaStreamResolver(
            @Autowired(required = false) FileStorageService fileStorageService,
            @Autowired(required = false) com.crescendo.storage.security.UrlSecurityValidator urlSecurityValidator) {
        this.fileStorageService = fileStorageService;
        this.urlSecurityValidator = urlSecurityValidator != null
                ? urlSecurityValidator
                : new com.crescendo.storage.security.UrlSecurityValidator();
    }

    public record MediaSource(
            InputStream stream,
            long contentLength,
            String contentType,
            String filename
    ) implements AutoCloseable {
        @Override
        public void close() {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    logger.debug("Error closing media stream: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Resolves a media input object or string into an open MediaSource stream.
     *
     * @param input raw input (URL string, Drive link, storageKey, JSON metadata map/string, or base64)
     * @param fallbackMimeType default MIME type if undetermined
     * @return MediaSource containing the open stream, size, and MIME type
     */
    public MediaSource resolve(Object input, String fallbackMimeType) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Media input cannot be null");
        }

        String defaultMime = (fallbackMimeType != null && !fallbackMimeType.isBlank()) ? fallbackMimeType : "application/octet-stream";

        // 1. Check if input is a Map (structured storage reference from frontend)
        if (input instanceof Map<?, ?> map) {
            if (map.containsKey("storageKey")) {
                String key = String.valueOf(map.get("storageKey"));
                String name = map.get("name") != null ? String.valueOf(map.get("name")) : "file";
                String type = map.get("contentType") != null ? String.valueOf(map.get("contentType")) : defaultMime;
                long size = map.get("sizeBytes") instanceof Number n ? n.longValue() : -1L;
                return resolveStorageKey(key, name, type, size);
            }
            if (map.containsKey("url")) {
                return resolveUrl(String.valueOf(map.get("url")), defaultMime);
            }
        }

        String str = input.toString().trim();

        // 2. Check if JSON string with storageKey
        if (str.startsWith("{") && str.endsWith("}")) {
            try {
                Map<?, ?> json = MAPPER.readValue(str, Map.class);
                if (json.containsKey("storageKey")) {
                    String key = String.valueOf(json.get("storageKey"));
                    String name = json.get("name") != null ? String.valueOf(json.get("name")) : "file";
                    String type = json.get("contentType") != null ? String.valueOf(json.get("contentType")) : defaultMime;
                    long size = json.get("sizeBytes") instanceof Number n ? n.longValue() : -1L;
                    return resolveStorageKey(key, name, type, size);
                }
                if (json.containsKey("url")) {
                    return resolveUrl(String.valueOf(json.get("url")), defaultMime);
                }
            } catch (Exception ignored) {}
        }

        // 3. Check for Google Drive URL
        Matcher driveMatcher = GOOGLE_DRIVE_PATTERN.matcher(str);
        if (driveMatcher.find()) {
            String fileId = driveMatcher.group(1);
            String directDownloadUrl = "https://drive.google.com/uc?export=download&id=" + fileId + "&confirm=t";
            logger.info("[MediaStreamResolver] Resolved Google Drive link to direct download: fileId={}", fileId);
            return resolveUrl(directDownloadUrl, defaultMime);
        }

        // 4. Check for standard HTTP / HTTPS URL
        if (str.startsWith("http://") || str.startsWith("https://")) {
            return resolveUrl(str, defaultMime);
        }

        // 5. Check if string is a storageKey (UUID format)
        if (str.matches("^[0-9a-fA-F-]{36}$") && fileStorageService != null) {
            return resolveStorageKey(str, "file", defaultMime, -1L);
        }

        // 6. Base64 data (with or without data: URL prefix)
        String rawBase64 = str;
        String detectedMime = defaultMime;
        if (str.startsWith("data:")) {
            int commaIdx = str.indexOf(",");
            if (commaIdx > 0) {
                String meta = str.substring(5, commaIdx);
                if (meta.contains(";")) {
                    detectedMime = meta.substring(0, meta.indexOf(";"));
                }
                rawBase64 = str.substring(commaIdx + 1);
            }
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(rawBase64.replaceAll("\\s+", ""));
            return new MediaSource(
                    new ByteArrayInputStream(bytes),
                    bytes.length,
                    detectedMime,
                    "media_data"
            );
        } catch (IllegalArgumentException e) {
            throw new IOException("Unable to resolve media source: Invalid URL, storage key, or Base64 content", e);
        }
    }

    private MediaSource resolveStorageKey(String storageKey, String filename, String contentType, long sizeBytes) throws IOException {
        if (fileStorageService == null) {
            throw new IOException("FileStorageService is not configured for storageKey resolution");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fileStorageService.streamContent(storageKey, baos);
        byte[] bytes = baos.toByteArray();
        return new MediaSource(
                new ByteArrayInputStream(bytes),
                sizeBytes > 0 ? sizeBytes : bytes.length,
                contentType,
                filename
        );
    }

    private MediaSource resolveUrl(String urlStr, String defaultMime) throws IOException {
        if (urlSecurityValidator != null) {
            urlSecurityValidator.validateUrl(urlStr);
        }

        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false); // Handle redirects explicitly to re-validate SSRF
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);
        conn.setRequestProperty("User-Agent", "Crescendo-StreamResolver/1.0");

        int status = conn.getResponseCode();
        // Handle redirect with full SSRF check on the redirect location
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
            String newUrl = conn.getHeaderField("Location");
            if (newUrl != null && !newUrl.isBlank()) {
                conn.disconnect();
                if (urlSecurityValidator != null) {
                    urlSecurityValidator.validateUrl(newUrl);
                }
                return resolveUrl(newUrl, defaultMime);
            }
        }

        if (status >= 400) {
            throw new IOException("Failed to fetch media from URL (" + status + "): " + urlStr);
        }

        long length = conn.getContentLengthLong();
        String contentType = conn.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = defaultMime;
        } else if (contentType.contains(";")) {
            contentType = contentType.substring(0, contentType.indexOf(";")).trim();
        }

        String filename = "downloaded_media";
        String disposition = conn.getHeaderField("Content-Disposition");
        if (disposition != null && disposition.contains("filename=")) {
            filename = disposition.substring(disposition.indexOf("filename=") + 9).replace("\"", "").trim();
        } else {
            String path = url.getPath();
            if (path != null && path.contains("/")) {
                String lastSeg = path.substring(path.lastIndexOf('/') + 1);
                if (!lastSeg.isBlank() && lastSeg.contains(".")) {
                    filename = lastSeg;
                }
            }
        }

        return new MediaSource(
                conn.getInputStream(),
                length,
                contentType,
                filename
        );
    }
}
