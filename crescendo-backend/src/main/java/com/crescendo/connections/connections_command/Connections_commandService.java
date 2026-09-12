package com.crescendo.connections.connections_command;

import com.crescendo.app.App;
import com.crescendo.app.AppRepository;
import com.crescendo.connections.ConnectionsDto;
import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import com.crescendo.connections.connections_query.Connections_query;
import com.crescendo.connections.connections_query.Connections_queryRepository;
import com.crescendo.connections.domain_event.ConnectionCreatedEvent;
import com.crescendo.connections.domain_event.ConnectionDeletedEvent;
import com.crescendo.connections.domain_event.ConnectionUpdatedEvent;
import com.crescendo.connections.oauth.OAuthTokenRefreshService;
import com.crescendo.enums.ConnectionStatus;
import com.crescendo.enums.AuthType;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.shared.domain.valueobject.AppKey;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Write-side service for connection management.
 *
 * Every mutation:
 *   1. Validates ownership
 *   2. Writes to the command database
 *   3. Synchronously projects to the query database
 *   4. Publishes a domain event
 */
@Service
@Transactional
public class Connections_commandService {

    private static final Logger logger = LoggerFactory.getLogger(Connections_commandService.class);

    private final Connections_commandRepository connectionRepo;
    private final Connections_queryRepository connectionQueryRepo;
    private final User_commandRepository userRepo;
    private final AppRepository appRepository;
    private final ConnectionCredentialsCryptoService cryptoService;
    private final DomainEventPublisher eventPublisher;
    private final OAuthTokenRefreshService tokenRefreshService;
    private final com.crescendo.emailservice.domain.DomainRepository domainRepository;
    private final RestClient restClient;

    public Connections_commandService(Connections_commandRepository connectionRepo,
                                     Connections_queryRepository connectionQueryRepo,
                                     User_commandRepository userRepo,
                                     AppRepository appRepository,
                                     ConnectionCredentialsCryptoService cryptoService,
                                     DomainEventPublisher eventPublisher,
                                     OAuthTokenRefreshService tokenRefreshService,
                                     com.crescendo.emailservice.domain.DomainRepository domainRepository) {
        this.connectionRepo = connectionRepo;
        this.connectionQueryRepo = connectionQueryRepo;
        this.userRepo = userRepo;
        this.appRepository = appRepository;
        this.cryptoService = cryptoService;
        this.eventPublisher = eventPublisher;
        this.tokenRefreshService = tokenRefreshService;
        this.domainRepository = domainRepository;
        this.restClient = RestClient.create();
    }

    /**
     * Creates a new connection for a user.
     */
    public ConnectionsDto.ConnectionResponse createConnection(UUID userId,
                                                               ConnectionsDto.CreateConnectionRequest req) {
        User_command user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        AppKey appKey = AppKey.of(req.appKey());
        App app = appRepository.findById(appKey)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown appKey: " + req.appKey()));
        validateCredentialsForAuthType(app, req.credentials());
        var encryptedCredentials = cryptoService.seal(req.credentials());

        List<Connections_command> existingList = connectionRepo.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(c -> req.appKey().equals(c.getAppKey()))
                .toList();

        Connections_command connection;
        UUID connectionId;
        if (!existingList.isEmpty()) {
            connection = existingList.get(0);
            connectionId = connection.getId();
            connection.setName(req.name());
            connection.setCredentials(encryptedCredentials);
            connection.setStatus(ConnectionStatus.ACTIVE);
            connectionRepo.save(connection);

            connectionQueryRepo.findById(connectionId).ifPresent(q -> {
                q.setName(req.name());
                q.setStatus(ConnectionStatus.ACTIVE);
                connectionQueryRepo.save(q);
            });
            eventPublisher.publish(new ConnectionUpdatedEvent(connectionId));
        } else {
            connectionId = UUID.randomUUID();
            connection = new Connections_command(
                connectionId, user, appKey, req.name(), encryptedCredentials, ConnectionStatus.ACTIVE);
            connectionRepo.save(connection);
            projectToQuery(connection, userId);
            eventPublisher.publish(new ConnectionCreatedEvent(connectionId, userId, req.appKey()));
        }

        return toResponse(connection, userId);
    }

    /**
     * Updates a connection's name and/or credentials.
     */
    public void updateConnection(UUID userId, UUID connectionId,
                                 ConnectionsDto.UpdateConnectionRequest req) {
        Connections_command connection = findOwnedConnection(userId, connectionId);

        if (req.name() == null && req.credentials() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field must be provided");
        }

        if (req.name() != null) {
            connection.setName(req.name());
        }
        if (req.credentials() != null) {
            App app = appRepository.findById(AppKey.of(connection.getAppKey()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown appKey: " + connection.getAppKey()));
            validateCredentialsForAuthType(app, req.credentials());
            connection.setCredentials(cryptoService.seal(req.credentials()));
            connection.setStatus(ConnectionStatus.ACTIVE);
        }

        // Sync name/status to query side
        connectionQueryRepo.findById(connectionId).ifPresent(q -> {
            boolean modified = false;
            if (req.name() != null) {
                q.setName(req.name());
                modified = true;
            }
            if (req.credentials() != null) {
                q.setStatus(ConnectionStatus.ACTIVE);
                modified = true;
            }
            if (modified) {
                connectionQueryRepo.save(q);
            }
        });

        eventPublisher.publish(new ConnectionUpdatedEvent(connectionId));
    }

    /**
     * Deletes a connection permanently.
     */
    public void deleteConnection(UUID userId, UUID connectionId) {
        Connections_command connection = findOwnedConnection(userId, connectionId);

        if (domainRepository.existsByEmailProviderConnectionId(connectionId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete connection because it is currently used as a personal email provider by one or more verified domains.");
        }

        connectionRepo.delete(connection);
        connectionQueryRepo.deleteById(connectionId);

        eventPublisher.publish(new ConnectionDeletedEvent(connectionId));
    }

    /**
     * Tests a connection by making a lightweight API call to the provider.
     * Returns a map with {success: bool, message: string, provider: string}.
     */
    public Map<String, Object> testConnection(UUID userId, UUID connectionId) {
        Connections_command connection = findOwnedConnection(userId, connectionId);
        String appKey = connection.getAppKey();

        // Get valid (auto-refreshed) credentials
        Map<String, Object> credentials = tokenRefreshService.getValidCredentials(connection);

        Map<String, Object> result = new HashMap<>();
        result.put("provider", appKey);

        try {
            String testMessage = performTestCall(appKey, credentials);
            result.put("success", true);
            result.put("message", testMessage);
            logger.info("[test-connection] {} connection {} — OK", appKey, connectionId);
        } catch (Exception e) {
            // If the call failed, it might be an expired token that wasn't caught
            // because tokenExpiresAt was missing. Try refreshing once and retry.
            String refreshToken = credentials.get("refreshToken") != null
                    ? credentials.get("refreshToken").toString() : null;
            if (refreshToken != null && !refreshToken.isBlank()) {
                try {
                    logger.info("[test-connection] Retrying {} after token refresh", appKey);
                    // Force re-read from DB (another path may have refreshed)
                    connection = findOwnedConnection(userId, connectionId);
                    credentials = tokenRefreshService.getValidCredentials(connection);
                    String testMessage = performTestCall(appKey, credentials);
                    result.put("success", true);
                    result.put("message", testMessage);
                    logger.info("[test-connection] {} connection {} — OK (after refresh)", appKey, connectionId);
                } catch (Exception retryErr) {
                    result.put("success", false);
                    result.put("message", "Connection failed: " + retryErr.getMessage());
                    logger.warn("[test-connection] {} connection {} — FAILED after retry: {}", appKey, connectionId, retryErr.getMessage());
                }
            } else {
                result.put("success", false);
                result.put("message", "Connection failed: " + e.getMessage());
                logger.warn("[test-connection] {} connection {} — FAILED: {}", appKey, connectionId, e.getMessage());
            }
        }
        return result;
    }

    /**
     * Tests connection credentials directly before saving.
     */
    public Map<String, Object> testCredentials(String appKey, Map<String, Object> credentials) {
        Map<String, Object> result = new HashMap<>();
        result.put("provider", appKey);
        try {
            String testMessage = performTestCall(appKey, credentials != null ? credentials : Map.of());
            result.put("success", true);
            result.put("message", testMessage);
            logger.info("[test-credentials] {} — OK", appKey);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            logger.warn("[test-credentials] {} — FAILED: {}", appKey, e.getMessage());
        }
        return result;
    }

    /**
     * Dispatches a lightweight test call based on the app provider.
     */
    private String performTestCall(String appKey, Map<String, Object> credentials) {
        return switch (appKey) {
            // ── Slack ──
            case "slack" -> {
                String token = getToken(credentials, "accessToken", "botToken");
                String resp = restClient.get()
                        .uri("https://slack.com/api/auth.test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "Slack: authenticated" + (resp != null && resp.contains("\"ok\":true") ? " ✓" : "");
            }

            // ── Discord ──
            case "discord" -> {
                String botToken = str(credentials.get("botToken"));
                String accessToken = str(credentials.get("accessToken"));
                String authHeader = (botToken != null && !botToken.isBlank())
                        ? "Bot " + botToken : "Bearer " + accessToken;
                restClient.get()
                        .uri("https://discord.com/api/v10/users/@me")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .retrieve().body(String.class);
                yield "Discord: authenticated ✓";
            }

            // ── Google apps ──
            case "gmail", "google-sheets", "google-calendar", "google-drive",
                 "google-docs", "google-forms", "google-slides", "google-tasks" -> {
                String token = getToken(credentials, "accessToken");
                restClient.get()
                        .uri("https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=" + token)
                        .retrieve().body(String.class);
                yield appKey + ": Google token valid ✓";
            }

            // ── Microsoft apps ──
            case "microsoft-outlook", "microsoft-teams", "microsoft-excel" -> {
                String token = getToken(credentials, "accessToken");
                restClient.get()
                        .uri("https://graph.microsoft.com/v1.0/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield appKey + ": Microsoft Graph authenticated ✓";
            }

            // ── GitHub ──
            case "github" -> {
                String token = getToken(credentials, "accessToken");
                restClient.get()
                        .uri("https://api.github.com/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "GitHub: authenticated ✓";
            }

            // ── GitLab ──
            case "gitlab" -> {
                String token = getToken(credentials, "accessToken");
                restClient.get()
                        .uri("https://gitlab.com/api/v4/user")
                        .header("PRIVATE-TOKEN", token)
                        .retrieve().body(String.class);
                yield "GitLab: authenticated ✓";
            }

            // ── Notion ──
            case "notion" -> {
                String token = getToken(credentials, "accessToken");
                restClient.get()
                        .uri("https://api.notion.com/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Notion-Version", "2022-06-28")
                        .retrieve().body(String.class);
                yield "Notion: authenticated ✓";
            }

            // ── Spotify ──
            case "spotify" -> {
                String token = getToken(credentials, "accessToken");
                restClient.get()
                        .uri("https://api.spotify.com/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "Spotify: authenticated ✓";
            }

            // ── OpenAI ──
            case "openai" -> {
                String key = getToken(credentials, "apiKey");
                restClient.get()
                        .uri("https://api.openai.com/v1/models")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                        .retrieve().body(String.class);
                yield "OpenAI: API key valid ✓";
            }

            // ── Gemini ──
            case "gemini" -> {
                String key = getToken(credentials, "apiKey");
                restClient.get()
                        .uri("https://generativelanguage.googleapis.com/v1beta/models?key=" + key)
                        .retrieve().body(String.class);
                yield "Gemini: API key valid ✓";
            }

            // ── Sarvam ──
            case "sarvam" -> {
                String key = getToken(credentials, "apiKey", "api-subscription-key");
                if (key.isBlank()) throw new RuntimeException("API key is empty");
                yield "Sarvam: API key configured ✓";
            }

            // ── Telegram ──
            case "telegram" -> {
                String token = getToken(credentials, "botToken");
                restClient.get()
                        .uri("https://api.telegram.org/bot" + token + "/getMe")
                        .retrieve().body(String.class);
                yield "Telegram: bot authenticated ✓";
            }

            // ── Airtable ──
            case "airtable" -> {
                String token = getToken(credentials, "accessToken", "apiKey");
                restClient.get()
                        .uri("https://api.airtable.com/v0/meta/whoami")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "Airtable: authenticated ✓";
            }

            // ── Linear ──
            case "linear" -> {
                String token = getToken(credentials, "accessToken");
                restClient.post()
                        .uri("https://api.linear.app/graphql")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("Content-Type", "application/json")
                        .body("{\"query\":\"{viewer{id name}}\"}")
                        .retrieve().body(String.class);
                yield "Linear: authenticated ✓";
            }

            // ── Toggl ──
            case "toggl" -> {
                String token = getToken(credentials, "apiKey", "apiToken");
                restClient.get()
                        .uri("https://api.track.toggl.com/api/v9/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Basic " + java.util.Base64.getEncoder().encodeToString((token + ":api_token").getBytes()))
                        .retrieve().body(String.class);
                yield "Toggl: authenticated ✓";
            }

            // ── Strava ──
            case "strava" -> {
                String token = getToken(credentials, "accessToken");
                restClient.get()
                        .uri("https://www.strava.com/api/v3/athlete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "Strava: authenticated ✓";
            }

            // ── Figma ──
            case "figma" -> {
                String token = getToken(credentials, "accessToken");
                restClient.get()
                        .uri("https://api.figma.com/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "Figma: authenticated ✓";
            }

            // ── LinkedIn ──
            case "linkedin" -> {
                String token = getToken(credentials, "accessToken");
                restClient.get()
                        .uri("https://api.linkedin.com/v2/userinfo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "LinkedIn: authenticated ✓";
            }

            // ── Twitter/X ──
            case "twitter" -> {
                String token = getToken(credentials, "accessToken");
                restClient.get()
                        .uri("https://api.twitter.com/2/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "Twitter/X: authenticated ✓";
            }

            // ── Mailchimp ──
            case "mailchimp" -> {
                String apiKey = getToken(credentials, "apiKey", "accessToken");
                String dc = "us1";
                if (apiKey.contains("-")) {
                    dc = apiKey.substring(apiKey.lastIndexOf('-') + 1);
                }
                String basicAuth = java.util.Base64.getEncoder().encodeToString(("anystring:" + apiKey).getBytes());
                restClient.get()
                        .uri("https://" + dc + ".api.mailchimp.com/3.0/ping")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                        .retrieve().body(String.class);
                yield "Mailchimp: API key valid ✓";
            }

            // ── HubSpot ──
            case "hubspot" -> {
                String token = getToken(credentials, "apiKey", "accessToken");
                restClient.get()
                        .uri("https://api.hubapi.com/crm/v3/objects/contacts?limit=1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "HubSpot: authenticated ✓";
            }

            // ── Todoist ──
            case "todoist" -> {
                String token = getToken(credentials, "apiKey", "accessToken");
                restClient.get()
                        .uri("https://api.todoist.com/rest/v2/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "Todoist: authenticated ✓";
            }

            // ── Trello ──
            case "trello" -> {
                String key = getToken(credentials, "apiKey", "key");
                String token = getToken(credentials, "apiToken", "token", "accessToken");
                restClient.get()
                        .uri("https://api.trello.com/1/members/me?key=" + key + "&token=" + token)
                        .retrieve().body(String.class);
                yield "Trello: authenticated ✓";
            }

            // ── Asana ──
            case "asana" -> {
                String token = getToken(credentials, "accessToken", "apiKey");
                restClient.get()
                        .uri("https://app.asana.com/api/1.0/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "Asana: authenticated ✓";
            }

            // ── Brevo ──
            case "brevo" -> {
                String key = getToken(credentials, "apiKey");
                restClient.get()
                        .uri("https://api.brevo.com/v3/account")
                        .header("api-key", key)
                        .retrieve().body(String.class);
                yield "Brevo: authenticated ✓";
            }

            // ── Dropbox ──
            case "dropbox" -> {
                String token = getToken(credentials, "accessToken");
                restClient.post()
                        .uri("https://api.dropboxapi.com/2/users/get_current_account")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "Dropbox: authenticated ✓";
            }

            // ── Typeform ──
            case "typeform" -> {
                String token = getToken(credentials, "accessToken");
                restClient.get()
                        .uri("https://api.typeform.com/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                yield "Typeform: authenticated ✓";
            }

            // ── Weather (OpenWeatherMap) ──
            case "weather" -> {
                String key = getToken(credentials, "apiKey");
                restClient.get()
                        .uri("https://api.openweathermap.org/data/2.5/weather?q=London&appid=" + key)
                        .retrieve().body(String.class);
                yield "Weather: API key valid ✓";
            }

            // ── Marketstack ──
            case "marketstack" -> {
                String key = getToken(credentials, "accessKey", "apiKey");
                restClient.get()
                        .uri("https://api.marketstack.com/v1/eod/latest?access_key=" + key + "&symbols=AAPL&limit=1")
                        .retrieve().body(String.class);
                yield "Marketstack: access key valid ✓";
            }

            // ── Giphy ──
            case "giphy" -> {
                String key = getToken(credentials, "apiKey");
                restClient.get()
                        .uri("https://api.giphy.com/v1/gifs/trending?api_key=" + key + "&limit=1")
                        .retrieve().body(String.class);
                yield "Giphy: API key valid ✓";
            }

            // ── Postgres / PostgreSQL ──
            case "postgres", "postgresql" -> {
                String host = str(credentials.getOrDefault("host", "localhost"));
                String port = str(credentials.getOrDefault("port", "5432"));
                String database = str(credentials.getOrDefault("database", "postgres"));
                String user = str(credentials.getOrDefault("user", credentials.get("username")));
                if (user == null || user.isBlank()) user = "postgres";
                String password = str(credentials.getOrDefault("password", ""));
                String sslMode = str(credentials.getOrDefault("sslMode", "prefer"));

                String url = "jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=" + sslMode + "&loginTimeout=6";
                java.util.Properties props = new java.util.Properties();
                props.setProperty("user", user);
                props.setProperty("password", password);
                try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, props);
                     java.sql.Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(5);
                    stmt.execute("SELECT 1");
                    yield "Postgres: connected successfully (" + conn.getMetaData().getDatabaseProductVersion() + ") ✓";
                } catch (Exception e) {
                    throw new RuntimeException("Postgres connection failed: " + e.getMessage(), e);
                }
            }

            // ── MySQL ──
            case "mysql" -> {
                String host = str(credentials.getOrDefault("host", "localhost"));
                String port = str(credentials.getOrDefault("port", "3306"));
                String database = str(credentials.getOrDefault("database", ""));
                String user = str(credentials.getOrDefault("username", credentials.get("user")));
                if (user == null || user.isBlank()) user = "root";
                String password = str(credentials.getOrDefault("password", ""));

                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?connectTimeout=5000&socketTimeout=5000";
                try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
                     java.sql.Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(5);
                    stmt.execute("SELECT 1");
                    yield "MySQL: connected successfully (" + conn.getMetaData().getDatabaseProductVersion() + ") ✓";
                } catch (Exception e) {
                    throw new RuntimeException("MySQL connection failed: " + e.getMessage(), e);
                }
            }

            // ── Redis ──
            case "redis" -> {
                String host = str(credentials.getOrDefault("host", "localhost"));
                int port = 6379;
                try {
                    port = Integer.parseInt(str(credentials.getOrDefault("port", 6379)));
                } catch (Exception ignored) {}
                try (java.net.Socket socket = new java.net.Socket()) {
                    socket.connect(new java.net.InetSocketAddress(host, port), 4000);
                    yield "Redis: reachable at " + host + ":" + port + " ✓";
                } catch (Exception e) {
                    throw new RuntimeException("Redis connection failed: " + e.getMessage(), e);
                }
            }

            // ── MongoDB ──
            case "mongodb", "mongoDb" -> {
                String uri = str(credentials.get("connectionString"));
                if (uri == null || uri.isBlank()) {
                    throw new RuntimeException("MongoDB connection string is required");
                }
                try (com.mongodb.client.MongoClient client = com.mongodb.client.MongoClients.create(uri)) {
                    client.getDatabase("admin").runCommand(new org.bson.Document("ping", 1));
                    yield "MongoDB: connected successfully ✓";
                } catch (Exception e) {
                    throw new RuntimeException("MongoDB connection failed: " + e.getMessage(), e);
                }
            }

            // ── Default (no-auth apps) ──
            default -> appKey + ": connection OK (no test endpoint available)";
        };
    }

    /**
     * Gets the first non-blank token from credentials for the given key names.
     */
    private String getToken(Map<String, Object> credentials, String... keys) {
        for (String key : keys) {
            Object val = credentials.get(key);
            if (val != null && !val.toString().isBlank()) {
                return val.toString();
            }
        }
        throw new RuntimeException("No valid token found (tried: " + String.join(", ", keys) + ")");
    }

    private String str(Object val) {
        return val != null ? val.toString() : null;
    }

    // QUERY-SIDE PROJECTION

    private void projectToQuery(Connections_command connection, UUID userId) {
        Connections_query q = new Connections_query(
                connection.getId(),
                userId,
                connection.getAppKey(),
                connection.getName(),
                connection.getStatus()
        );
        connectionQueryRepo.save(q);
    }

    // OWNERSHIP VERIFICATION

    private Connections_command findOwnedConnection(UUID userId, UUID connectionId) {
        return connectionRepo.findByIdAndUser_Id(connectionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found"));
    }

    private void validateCredentialsForAuthType(App app, java.util.Map<String, Object> credentials) {
        AuthType authType = app.getAuthType();
        if (authType == AuthType.NONE) {
            if (credentials != null && !credentials.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This app does not require credentials. Leave credentials empty.");
            }
            return;
        }

        if (credentials == null || credentials.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Credentials are required for " + app.getName());
        }

        // If the app defines a credentialSchema, validate that required fields are present
        List<Map<String, Object>> schema = app.getCredentialSchema();
        if (schema != null && !schema.isEmpty()) {
            for (Map<String, Object> field : schema) {
                Boolean required = (Boolean) field.getOrDefault("required", false);
                if (Boolean.TRUE.equals(required)) {
                    String key = (String) field.get("key");
                    Object val = credentials.get(key);
                    if (isBlankValue(val)) {
                        String label = (String) field.getOrDefault("label", key);
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                label + " is required for " + app.getName());
                    }
                }
            }
            return;
        }

        boolean hasUsableSecret = credentials.entrySet().stream()
                .anyMatch(e -> isSecretKey(e.getKey()) && !isBlankValue(e.getValue()));

        if (!hasUsableSecret) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Credentials must include at least one non-empty credential field (e.g., apiKey, token, password, secret)");
        }
    }

    private boolean isSecretKey(String key) {
        if (key == null) return false;
        String k = key.toLowerCase();
        return k.contains("token")
                || k.contains("secret")
                || k.contains("apikey")
                || k.contains("password")
                || k.contains("connectionstring")
                || k.contains("key")
                || k.equals("api_key")
                || k.equals("apiKey");
    }

    private boolean isBlankValue(Object value) {
        if (value == null) return true;
        if (value instanceof String s) return s.isBlank();
        return false;
    }

    // DTO MAPPER

    private ConnectionsDto.ConnectionResponse toResponse(Connections_command c, UUID userId) {
        return new ConnectionsDto.ConnectionResponse(
                c.getId(),
                c.getAppKey(),
                c.getName(),
                c.getStatus().name(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getGrantedScopes()
        );
    }
}
