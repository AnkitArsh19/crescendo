package com.crescendo.utils;

import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class RestClient {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private String method = "GET";
        private final Map<String, String> headers = new HashMap<>();
        private Object body;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder header(String key, String value) {
            if (key != null && value != null) {
                this.headers.put(key, value);
            }
            return this;
        }

        public Builder get() {
            this.method = "GET";
            return this;
        }

        public Builder post() {
            this.method = "POST";
            return this;
        }

        public Builder post(Object body) {
            return post().body(body);
        }

        public Builder put() {
            this.method = "PUT";
            return this;
        }

        public Builder put(Object body) {
            return put().body(body);
        }

        public Builder patch() {
            this.method = "PATCH";
            return this;
        }

        public Builder patch(Object body) {
            return patch().body(body);
        }

        public Builder delete() {
            this.method = "DELETE";
            return this;
        }

        public Builder body(Object body) {
            this.body = body;
            return this;
        }

        /** Adds a Basic Auth header (Base64-encodes user:password). */
        public Builder basicAuth(String username, String password) {
            String encoded = java.util.Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return header("Authorization", "Basic " + encoded);
        }

        /** Appends a query parameter to the URL. */
        public Builder queryParam(String name, String value) {
            if (name != null && value != null) {
                String encoded = java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
                this.url = (this.url != null ? this.url : "") +
                        (this.url != null && this.url.contains("?") ? "&" : "?") + name + "=" + encoded;
            }
            return this;
        }

        public Object execute() throws Exception {
            if (url == null) {
                throw new IllegalArgumentException("URL must not be null");
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url));
            
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }

            HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
            if (body != null) {
                if (!headers.containsKey("Content-Type") && !headers.containsKey("content-type")) {
                    requestBuilder.header("Content-Type", "application/json");
                }
                String jsonBody = OBJECT_MAPPER.writeValueAsString(body);
                bodyPublisher = HttpRequest.BodyPublishers.ofString(jsonBody);
            }

            requestBuilder.method(method, bodyPublisher);

            HttpResponse<String> response = HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();
            if (response.statusCode() >= 400) {
                throw new RuntimeException("HTTP Request failed with status code " + response.statusCode() + ": " + responseBody);
            }

            if (responseBody == null || responseBody.isBlank()) {
                return Map.of("status", "success", "statusCode", response.statusCode());
            }

            try {
                if (responseBody.trim().startsWith("[")) {
                    return OBJECT_MAPPER.readValue(responseBody, java.util.List.class);
                } else {
                    return OBJECT_MAPPER.readValue(responseBody, java.util.Map.class);
                }
            } catch (Exception e) {
                // If parsing fails, just return the raw string
                return responseBody;
            }
        }
    }
}
