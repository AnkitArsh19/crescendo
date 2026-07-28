package com.crescendo.execution.resource;

import com.crescendo.apps.gmail.GmailResourceProvider;
import com.crescendo.apps.slack.SlackResourceProvider;
import com.crescendo.apps.spotify.SpotifyResourceProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Template for connector resource tests. Each test talks only to an in-process
 * HTTP fixture, while asserting the same request and response contract used in
 * production. Add a new connector by adding another fixture context and case.
 */
class ResourceProviderHttpContractTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> authorization = new AtomicReference<>();

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

    @Test
    void gmailLabels_usesBearerToken_andSortsOptionsByLabel() {
        server.createContext("/gmail/v1/users/me/labels", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, """
                    {"labels":[
                      {"id":"INBOX","name":"Inbox","type":"system"},
                      {"id":"Label_1","name":"Alpha","type":"user"}
                    ]}
                    """);
        });

        GmailResourceProvider provider = new GmailResourceProvider(baseUrl + "/gmail/v1");
        List<ResourceOption> options = provider.listResources(
                Map.of("accessToken", "gmail-test-token"), "labels", Map.of());

        assertEquals("Bearer gmail-test-token", authorization.get());
        assertEquals(List.of("Alpha", "Inbox"), options.stream().map(ResourceOption::label).toList());
        assertEquals("Label_1", options.getFirst().id());
    }

    @Test
    void spotifyPlaylists_usesBearerToken_andMapsTrackCount() {
        server.createContext("/v1/me/playlists", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            assertEquals("limit=50", exchange.getRequestURI().getQuery());
            respond(exchange, """
                    {"items":[{"id":"playlist-1","name":"Study flow","tracks":{"total":42}}]}
                    """);
        });

        SpotifyResourceProvider provider = new SpotifyResourceProvider(baseUrl + "/v1");
        List<ResourceOption> options = provider.listResources(
                Map.of("accessToken", "spotify-test-token"), "playlists", Map.of());

        assertEquals("Bearer spotify-test-token", authorization.get());
        assertEquals(1, options.size());
        assertEquals("playlist-1", options.getFirst().id());
        assertEquals("Study flow", options.getFirst().label());
        assertEquals("42 tracks", options.getFirst().description());
    }

    @Test
    void slackChannels_usesBearerToken_andFormatsChannelNames() {
        server.createContext("/conversations.list", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, """
                    {
                      "ok": true,
                      "channels": [
                        {"id": "C123", "name": "general", "is_private": false},
                        {"id": "C456", "name": "secret-project", "is_private": true}
                      ]
                    }
                    """);
        });

        SlackResourceProvider provider = new SlackResourceProvider(baseUrl);
        List<ResourceOption> options = provider.listResources(
                Map.of("accessToken", "slack-test-token"), "channels", Map.of());

        assertEquals("Bearer slack-test-token", authorization.get());
        assertEquals(2, options.size());
        assertEquals("C123", options.get(0).id());
        assertEquals("#general", options.get(0).label());
        assertEquals("# · C123", options.get(0).description());
        assertEquals("C456", options.get(1).id());
        assertEquals("#secret-project", options.get(1).label());
        assertEquals("🔒 Private · C456", options.get(1).description());
    }


    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
