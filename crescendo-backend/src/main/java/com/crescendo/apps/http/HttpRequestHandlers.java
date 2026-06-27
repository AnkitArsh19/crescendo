package com.crescendo.apps.http;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ActionMapping(appKey = "http", actionKey = "request")
@SuppressWarnings("unchecked")
public class HttpRequestHandlers implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandlers.class);

    private final ObjectMapper objectMapper;

    public HttpRequestHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        if (config == null || config.get("url") == null || String.valueOf(config.get("url")).isBlank()) {
            return ActionResult.failure("HTTP action requires a url");
        }

        try {
            String method = String.valueOf(config.getOrDefault("method", "GET")).toUpperCase();
            
            // Extract advanced options
            Map<String, Object> options = config.containsKey("options") ? objectMap(config.get("options")) : Map.of();
            boolean neverError = booleanValue(options.get("neverError"), false);
            boolean fullResponse = booleanValue(options.get("fullResponse"), false);
            String responseFormat = String.valueOf(options.getOrDefault("responseFormat", "autodetect"));

            String paginationMode = String.valueOf(config.getOrDefault("paginationMode", "none"));
            int maxPages = Math.max(1, intValue(config.get("maxPages"), 1));
            if ("none".equalsIgnoreCase(paginationMode)) {
                maxPages = 1;
            }

            HttpClient client = buildClient(config, options);
            List<Map<String, Object>> responses = new ArrayList<>();
            for (int pageIndex = 0; pageIndex < maxPages; pageIndex++) {
                Map<String, Object> queryParams = resolveParams(config, "sendQuery", "specifyQuery", "queryParameters", "queryParams");
                applyPagination(queryParams, config, paginationMode, pageIndex);
                HttpRequest request = buildRequest(config, method, queryParams);
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                
                if (!neverError && response.statusCode() >= 400) {
                    return ActionResult.failure("HTTP error: " + response.statusCode() + " - " + new String(response.body() != null ? response.body() : new byte[0], StandardCharsets.UTF_8));
                }

                Map<String, Object> responseData = responseData(response, config, responseFormat, fullResponse);
                responses.add(responseData);

                Object body = responseData.get("body");
                if (Boolean.TRUE.equals(config.get("stopOnEmptyPage"))
                        && (body == null || String.valueOf(body).isBlank() || "[]".equals(String.valueOf(body).trim()))) {
                    break;
                }
            }

            if (responses.size() == 1) {
                return ActionResult.success(responses.getFirst());
            }
            Map<String, Object> output = new LinkedHashMap<>(responses.getLast());
            output.put("responses", responses);
            output.put("pageCount", responses.size());
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[http-action] Request failed", e);
            return ActionResult.failure("HTTP request failed: " + e.getMessage());
        }
    }

    private HttpClient buildClient(Map<String, Object> config, Map<String, Object> options) throws Exception {
        Map<String, Object> redirectObj = options.containsKey("redirect") ? objectMap(options.get("redirect")) : Map.of();
        boolean followRedirects = booleanValue(redirectObj.getOrDefault("followAllRedirects", config.get("followRedirects")), true);
        
        HttpClient.Builder builder = HttpClient.newBuilder()
                .followRedirects(followRedirects ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(Math.max(1, intValue(config.get("timeoutSeconds"), 30))));

        String proxy = String.valueOf(options.get("proxy"));
        if (proxy != null && !proxy.isBlank() && !"null".equals(proxy)) {
            URI proxyUri = URI.create(proxy);
            builder.proxy(java.net.ProxySelector.of(new java.net.InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));
        }

        if (booleanValue(config.get("allowInsecureSsl"), false)) {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());
            builder.sslContext(sslContext);
        }

        return builder.build();
    }

    private HttpRequest buildRequest(Map<String, Object> config, String method, Map<String, Object> queryParams) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(withQueryParams(String.valueOf(config.get("url")), queryParams)))
                .timeout(Duration.ofSeconds(Math.max(1, intValue(config.get("timeoutSeconds"), 30))));

        Map<String, Object> headers = resolveParams(config, "sendHeaders", "specifyHeaders", "headerParameters", "headers");
        applyHeaders(builder, headers);
        applyAuth(builder, config, queryParams);

        HttpRequest.BodyPublisher body = bodyPublisher(config, builder);
        if ("GET".equals(method) || "HEAD".equals(method)) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, body);
        }
        return builder.build();
    }

    private HttpRequest.BodyPublisher bodyPublisher(Map<String, Object> config, HttpRequest.Builder builder) throws Exception {
        boolean sendBody = booleanValue(config.get("sendBody"), true);
        if (!sendBody) {
            return HttpRequest.BodyPublishers.noBody();
        }
        
        String bodyType = String.valueOf(config.getOrDefault("bodyType", config.containsKey("body") ? "json" : "none")).toLowerCase();
        return switch (bodyType) {
            case "raw" -> {
                builder.header("Content-Type", String.valueOf(config.getOrDefault("contentType", "text/plain")));
                yield HttpRequest.BodyPublishers.ofString(String.valueOf(config.getOrDefault("rawBody", "")));
            }
            case "form", "form-urlencoded" -> {
                builder.header("Content-Type", "application/x-www-form-urlencoded");
                Map<String, Object> formData = resolveParams(config, "sendBody", "specifyBody", "bodyParameters", "formData");
                yield HttpRequest.BodyPublishers.ofString(urlEncoded(formData));
            }
            case "multipart", "multipart-form-data" -> multipartBody(config, builder);
            case "json" -> {
                builder.header("Content-Type", "application/json");
                Map<String, Object> bodyParams = resolveParams(config, "sendBody", "specifyBody", "bodyParameters", "body");
                yield HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(bodyParams));
            }
            default -> HttpRequest.BodyPublishers.noBody();
        };
    }

    private HttpRequest.BodyPublisher multipartBody(Map<String, Object> config, HttpRequest.Builder builder) throws Exception {
        String boundary = "crescendo-" + UUID.randomUUID();
        builder.header("Content-Type", "multipart/form-data; boundary=" + boundary);
        List<byte[]> parts = new ArrayList<>();
        for (Map.Entry<String, Object> entry : objectMap(config.get("formData")).entrySet()) {
            parts.add(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n"
                    + String.valueOf(entry.getValue()) + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        Object fileObj = config.get("file");
        if (fileObj instanceof Map<?, ?> file) {
            String fieldName = String.valueOf(config.getOrDefault("fileFieldName", "file"));
            Object nameValue = file.containsKey("name") ? file.get("name") : "upload.bin";
            Object typeValue = file.containsKey("type") ? file.get("type") : "application/octet-stream";
            String fileName = String.valueOf(nameValue);
            String contentType = String.valueOf(typeValue);
            byte[] bytes = decodeDataUri(String.valueOf(file.get("data")));
            parts.add(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            parts.add(bytes);
            parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(parts);
    }

    private Map<String, Object> responseData(HttpResponse<byte[]> response, Map<String, Object> config, String responseFormat, boolean fullResponse) {
        Map<String, Object> output = new LinkedHashMap<>();
        byte[] bodyBytes = response.body() == null ? new byte[0] : response.body();
        
        Object parsedBody = null;
        if ("binary".equalsIgnoreCase(String.valueOf(config.getOrDefault("responseType", "text"))) || "file".equalsIgnoreCase(responseFormat)) {
            Map<String, Object> binaryData = new LinkedHashMap<>();
            binaryData.put("data", Base64.getEncoder().encodeToString(bodyBytes));
            binaryData.put("mimeType", response.headers().firstValue("content-type").orElse("application/octet-stream"));
            binaryData.put("fileSize", bodyBytes.length);
            parsedBody = binaryData;
        } else {
            String text = new String(bodyBytes, StandardCharsets.UTF_8);
            if ("json".equalsIgnoreCase(responseFormat) || ("autodetect".equalsIgnoreCase(responseFormat) && response.headers().firstValue("content-type").orElse("").contains("json"))) {
                try {
                    parsedBody = objectMapper.readValue(text, Object.class);
                } catch (Exception e) {
                    parsedBody = text;
                }
            } else {
                parsedBody = text;
            }
        }

        if (fullResponse) {
            output.put("statusCode", response.statusCode());
            output.put("statusMessage", "OK");
            output.put("headers", response.headers().map());
            output.put("body", parsedBody);
            return output;
        }

        if (parsedBody instanceof Map<?, ?> m) {
            Map<String, Object> result = new LinkedHashMap<>();
            m.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }

        output.put("body", parsedBody);
        return output;
    }

    private void applyHeaders(HttpRequest.Builder builder, Map<String, Object> headers) {
        headers.forEach((key, value) -> {
            if (value != null && !"Content-Length".equalsIgnoreCase(key)) {
                builder.header(key, String.valueOf(value));
            }
        });
    }

    private void applyAuth(HttpRequest.Builder builder, Map<String, Object> config, Map<String, Object> queryParams) {
        String authType = String.valueOf(config.getOrDefault("authentication", "none"));
        if ("basicAuth".equalsIgnoreCase(authType) || (config.containsKey("basicAuth") && !String.valueOf(config.get("basicAuth")).isBlank())) {
            Object basicAuth = config.get("basicAuth");
            if (basicAuth != null && !String.valueOf(basicAuth).isBlank()) {
                String encoded = Base64.getEncoder().encodeToString(String.valueOf(basicAuth).getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + encoded);
            }
        } else if ("bearerAuth".equalsIgnoreCase(authType)) {
            Object bearerAuth = config.get("bearerAuth");
            if (bearerAuth != null && !String.valueOf(bearerAuth).isBlank()) {
                builder.header("Authorization", "Bearer " + bearerAuth);
            }
        } else if ("headerAuth".equalsIgnoreCase(authType)) {
            Object headerName = config.get("headerAuthName");
            Object headerValue = config.get("headerAuthValue");
            if (headerName != null && headerValue != null) {
                builder.header(String.valueOf(headerName), String.valueOf(headerValue));
            }
        } else if ("queryAuth".equalsIgnoreCase(authType)) {
            Object queryName = config.get("queryAuthName");
            Object queryValue = config.get("queryAuthValue");
            if (queryName != null && queryValue != null) {
                queryParams.put(String.valueOf(queryName), queryValue);
            }
        }
    }

    private void applyPagination(Map<String, Object> params, Map<String, Object> config, String mode, int pageIndex) {
        if ("page".equalsIgnoreCase(mode)) {
            params.put(String.valueOf(config.getOrDefault("pageParam", "page")), intValue(config.get("pageStart"), 1) + pageIndex);
        } else if ("offset".equalsIgnoreCase(mode)) {
            int limit = Math.max(1, intValue(config.get("limit"), 100));
            params.put(String.valueOf(config.getOrDefault("offsetParam", "offset")), intValue(config.get("offsetStart"), 0) + pageIndex * limit);
        }
        if (config.get("limit") != null) {
            params.put(String.valueOf(config.getOrDefault("limitParam", "limit")), config.get("limit"));
        }
    }

    private String withQueryParams(String url, Map<String, Object> params) {
        if (params.isEmpty()) return url;
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + urlEncoded(params);
    }

    private String urlEncoded(Map<String, Object> params) {
        List<String> pairs = new ArrayList<>();
        params.forEach((key, value) -> {
            if (value != null) {
                pairs.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8));
            }
        });
        return String.join("&", pairs);
    }

    private Map<String, Object> objectMap(Object value) throws Exception {
        if (value == null) return Map.of();
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, val) -> result.put(String.valueOf(key), val));
            return result;
        }
        if (value instanceof String str && !str.isBlank()) {
            return objectMapper.readValue(str, Map.class);
        }
        return Map.of();
    }

    private byte[] decodeDataUri(String value) {
        if (value == null || value.isBlank()) return new byte[0];
        int comma = value.indexOf(',');
        String base64 = comma >= 0 ? value.substring(comma + 1) : value;
        return Base64.getDecoder().decode(base64);
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number n) return n.intValue();
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean booleanValue(Object value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    private Map<String, Object> resolveParams(Map<String, Object> config, String sendFlag, String specifyFlag, String paramListKey, String legacyJsonObjKey) throws Exception {
        boolean send = booleanValue(config.getOrDefault(sendFlag, true), true);
        if (!send) return new LinkedHashMap<>();

        String specify = String.valueOf(config.getOrDefault(specifyFlag, "json"));
        if ("keypair".equalsIgnoreCase(specify)) {
            Object paramList = config.get(paramListKey);
            Map<String, Object> result = new LinkedHashMap<>();
            if (paramList instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        if (m.containsKey("name") && m.containsKey("value")) {
                            result.put(String.valueOf(m.get("name")), m.get("value"));
                        }
                    }
                }
            }
            return result;
        } else {
            // fallback to json object key if present, otherwise try parsing paramListKey as JSON string
            Object val = config.getOrDefault(paramListKey, config.get(legacyJsonObjKey));
            return new LinkedHashMap<>(objectMap(val));
        }
    }
}
