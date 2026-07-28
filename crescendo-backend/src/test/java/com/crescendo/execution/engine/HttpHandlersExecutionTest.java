package com.crescendo.execution.engine;

import com.crescendo.apps.http.HttpRequestHandlers;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HttpHandlersExecutionTest {

    private HttpServer server;
    private String baseUrl;
    private final HttpRequestHandlers handler = new HttpRequestHandlers(new ObjectMapper());
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private ActionContext createContext(Map<String, Object> config) {
        return new ActionContext("http", "request", config, Map.of(), Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    @Test
    @DisplayName("HTTP Handler executes GET request with Bearer authentication and parses JSON response")
    void executeGet_withBearerAuth_andJsonResponse() {
        server.createContext("/test-api", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{\"status\":\"ok\",\"count\":42}");
        });

        Map<String, Object> config = Map.of(
                "url", baseUrl + "/test-api",
                "method", "GET",
                "authentication", "bearerAuth",
                "bearerAuth", "secret-token-123"
        );

        ActionContext context = createContext(config);
        ActionResult result = handler.execute(context);

        assertTrue(result.success());
        assertEquals("Bearer secret-token-123", authorization.get());
        Map<String, Object> data = result.outputData();
        assertEquals("ok", data.get("status"));
        assertEquals(42, data.get("count"));
    }

    @Test
    @DisplayName("HTTP Handler executes POST request with JSON payload")
    void executePost_withJsonPayload() {
        server.createContext("/submit", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"created\":true}");
        });

        Map<String, Object> config = Map.of(
                "url", baseUrl + "/submit",
                "method", "POST",
                "bodyType", "raw",
                "contentType", "application/json",
                "rawBody", "{\"name\":\"test-workflow\"}"
        );

        ActionContext context = createContext(config);
        ActionResult result = handler.execute(context);

        assertTrue(result.success());
        assertEquals("{\"name\":\"test-workflow\"}", requestBody.get());
        assertEquals(true, result.outputData().get("created"));
    }

    @Test
    @DisplayName("HTTP Handler returns failure on 4xx/5xx errors when neverError is false")
    void execute_returnsFailureOnHttpErrors() {
        server.createContext("/error", exchange -> respond(exchange, 500, "Internal Server Error"));

        Map<String, Object> config = Map.of(
                "url", baseUrl + "/error",
                "method", "GET"
        );

        ActionContext context = createContext(config);
        ActionResult result = handler.execute(context);

        assertFalse(result.success());
        assertTrue(result.error().contains("500"));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}

