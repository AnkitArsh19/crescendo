package com.crescendo.connections.oauth;

import com.crescendo.app.App;
import com.crescendo.app.AppRepository;
import com.crescendo.connections.connections_command.Connections_command;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.connections_query.Connections_query;
import com.crescendo.connections.connections_query.Connections_queryRepository;
import com.crescendo.connections.domain_event.ConnectionCreatedEvent;
import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import com.crescendo.enums.ConnectionStatus;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.shared.domain.valueobject.AppKey;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

/**
 * Handles the OAuth2 authorization-code flow for integrating third-party apps.
 *
 * <p>
 * Flow:
 * <ol>
 * <li>Frontend calls {@code GET /connections/oauth/{provider}/authorize} →
 * redirect to provider</li>
 * <li>Provider redirects back to
 * {@code GET /connections/oauth/{provider}/callback} with code</li>
 * <li>This service exchanges the code for tokens, encrypts them, creates a
 * connection, and redirects to frontend</li>
 * </ol>
 *
 * <p>
 * Follows the same CQRS pattern as Connections_commandService:
 * write to command DB → project to query DB → publish domain event.
 *
 * <p>
 * PKCE (Proof Key for Code Exchange) is supported for providers that require it
 * (e.g. Airtable, Twitter/X). The code_verifier is stored in-memory keyed by
 * the OAuth state parameter and consumed during callback.
 *
 * <p><strong>Security:</strong> The OAuth state parameter is signed with HMAC-SHA256
 * using the server's crypto key. This prevents state forgery attacks where an attacker
 * could craft a state with a victim's userId to hijack the OAuth callback.
 */
@Service
public class IntegrationOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationOAuthService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MAX_PKCE_ENTRIES = 500;

    private final IntegrationOAuthConfig oauthConfig;
    private final Connections_commandRepository connectionRepo;
    private final Connections_queryRepository connectionQueryRepo;
    private final User_commandRepository userRepo;
    private final AppRepository appRepo;
    private final ConnectionCredentialsCryptoService cryptoService;
    private final DomainEventPublisher eventPublisher;
    private final RestTemplate restTemplate;
    private final byte[] hmacKeyBytes;

    /**
     * Bounded LRU cache for PKCE code_verifier values, keyed by OAuth state.
     * Entries are consumed (removed) during callback. Oldest entries are
     * automatically evicted when the cache exceeds {@link #MAX_PKCE_ENTRIES},
     * replacing the previous nuclear {@code ConcurrentHashMap.clear()} that
     * destroyed in-flight verifiers for all users.
     *
     * Thread-safety: wrapped with {@link Collections#synchronizedMap}.
     */
    private final Map<String, String> pkceVerifiers = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_PKCE_ENTRIES;
                }
            }
    );

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    public IntegrationOAuthService(IntegrationOAuthConfig oauthConfig,
            Connections_commandRepository connectionRepo,
            Connections_queryRepository connectionQueryRepo,
            User_commandRepository userRepo,
            AppRepository appRepo,
            ConnectionCredentialsCryptoService cryptoService,
            DomainEventPublisher eventPublisher,
            @Value("${credentials.crypto.key:}") String base64Key) {
        this.oauthConfig = oauthConfig;
        this.connectionRepo = connectionRepo;
        this.connectionQueryRepo = connectionQueryRepo;
        this.userRepo = userRepo;
        this.appRepo = appRepo;
        this.cryptoService = cryptoService;
        this.eventPublisher = eventPublisher;
        this.restTemplate = new RestTemplate();
        this.hmacKeyBytes = Base64.getDecoder().decode(base64Key.trim());
    }

    /**
     * Builds the OAuth authorization URL for the given provider.
     * The state parameter encodes the userId for use in the callback.
     * If the provider requires PKCE, generates code_verifier/code_challenge.
     */
    public String buildAuthorizationUrl(String providerKey, UUID userId) {
        IntegrationOAuthConfig.ProviderConfig config = getProviderConfig(providerKey);

        String redirectUri = backendUrl + "/connections/oauth/" + providerKey + "/callback";

        // State = userId:nonce:hmac(userId:nonce, serverSecret)
        // HMAC prevents state forgery — an attacker cannot craft a valid state
        // for another user's userId without knowing the server secret.
        String nonce = UUID.randomUUID().toString();
        String payload = userId.toString() + ":" + nonce;
        String signature = computeHmac(payload);
        String state = payload + ":" + signature;

        StringBuilder url = new StringBuilder(config.getAuthorizeUrl());
        url.append("?client_id=").append(encode(config.getClientId()));
        url.append("&redirect_uri=").append(encode(redirectUri));
        url.append("&response_type=code");
        url.append("&state=").append(encode(state));

        if (config.getScopes() != null && !config.getScopes().isBlank()) {
            url.append("&scope=").append(encode(config.getScopes()));
        }

        if (isGoogleProvider(providerKey)) {
            // Force the Google account chooser on each connect flow so users can attach
            // different Google apps to different accounts if they want to.
            url.append("&access_type=offline");
            url.append("&prompt=").append(encode("select_account consent"));
            url.append("&include_granted_scopes=false");
        }

        // PKCE support — required by Airtable, Twitter/X, and others
        if (config.isPkce()) {
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);
            url.append("&code_challenge=").append(encode(codeChallenge));
            url.append("&code_challenge_method=S256");

            // Bounded LRU cache — oldest entries evicted automatically, no nuclear clear()
            pkceVerifiers.put(state, codeVerifier);
        }

        return url.toString();
    }

    /**
     * Handles the OAuth callback: exchanges code for tokens, creates a connection.
     * Returns the frontend redirect URL.
     */
    @Transactional
    public String handleCallback(String providerKey, String code, String state) {
        IntegrationOAuthConfig.ProviderConfig config = getProviderConfig(providerKey);

        // Parse userId from state
        UUID userId = parseUserIdFromState(state);
        String redirectUri = backendUrl + "/connections/oauth/" + providerKey + "/callback";

        // Retrieve PKCE code_verifier if applicable
        String codeVerifier = pkceVerifiers.remove(state);

        // Exchange authorization code for tokens
        Map<String, Object> tokenResponse = exchangeCodeForTokens(config, code, redirectUri, providerKey, codeVerifier);

        // Encrypt and store credentials
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("accessToken", tokenResponse.get("access_token"));
        if (tokenResponse.containsKey("refresh_token")) {
            credentials.put("refreshToken", tokenResponse.get("refresh_token"));
        }
        if (tokenResponse.containsKey("expires_in")) {
            Object expiresIn = tokenResponse.get("expires_in");
            credentials.put("expiresIn", expiresIn);
            // Store absolute expiry so OAuthTokenRefreshService knows when to refresh
            long seconds = expiresIn instanceof Number n ? n.longValue() : Long.parseLong(expiresIn.toString());
            credentials.put("tokenExpiresAt", java.time.Instant.now().plusSeconds(seconds).toString());
        }
        if (tokenResponse.containsKey("token_type")) {
            credentials.put("tokenType", tokenResponse.get("token_type"));
        }

        // Resolve app key from provider key
        String appKey = resolveAppKey(providerKey);
        App app = appRepo.findById(AppKey.of(appKey))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App not found"));

        User_command user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Fetch account identity (email / display name) from provider's user-info API
        String accountEmail = null;
        String accountDisplayName = null;
        try {
            Map<String, String> identity = fetchAccountIdentity(providerKey, credentials);
            accountEmail = identity.get("email");
            accountDisplayName = identity.get("displayName");
        } catch (Exception e) {
            logger.warn("Could not fetch account identity for {}: {}", providerKey, e.getMessage());
        }

        // Build connection name: "Gmail · john@gmail.com" instead of generic "Gmail
        // Connection"
        String connectionName = app.getName();
        if (accountEmail != null && !accountEmail.isBlank()) {
            connectionName += " · " + accountEmail;
        } else if (accountDisplayName != null && !accountDisplayName.isBlank()) {
            connectionName += " · " + accountDisplayName;
        } else {
            connectionName += " Connection";
        }

        // Store identity in credentials for display purposes
        if (accountEmail != null)
            credentials.put("accountEmail", accountEmail);
        if (accountDisplayName != null)
            credentials.put("accountDisplayName", accountDisplayName);

        // Create connection (CQRS: command → query → event)
        UUID connectionId = UUID.randomUUID();
        var encryptedCredentials = cryptoService.seal(credentials);

        Connections_command connection = new Connections_command(
                connectionId, user, AppKey.of(appKey),
                connectionName,
                encryptedCredentials, ConnectionStatus.ACTIVE);
        connectionRepo.save(connection);

        // Project to query side
        Connections_query queryProjection = new Connections_query(
                connectionId, userId, appKey,
                connectionName, ConnectionStatus.ACTIVE);
        connectionQueryRepo.save(queryProjection);

        // Publish domain event
        eventPublisher.publish(new ConnectionCreatedEvent(connectionId, userId, appKey));

        logger.info("OAuth connection created for user {} via provider {} ({})",
                userId, providerKey, accountEmail != null ? accountEmail : "no-email");

        return frontendUrl + "/dashboard/connections?connected=" + appKey;
    }

    // ─── Token Exchange ────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeCodeForTokens(
            IntegrationOAuthConfig.ProviderConfig config,
            String code, String redirectUri, String providerKey,
            String codeVerifier) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        // Providers that require HTTP Basic auth for token exchange
        boolean useBasicAuth = "notion".equals(providerKey)
                || "reddit".equals(providerKey)
                || "airtable".equals(providerKey)
                || "twitter".equals(providerKey);

        if (useBasicAuth) {
            String basic = java.util.Base64.getEncoder().encodeToString(
                    (config.getClientId() + ":" + config.getClientSecret())
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basic);
        }

        // Reddit requires a custom User-Agent
        if ("reddit".equals(providerKey)) {
            headers.set("User-Agent", "crescendo:v1.0 (by /u/crescendo-app)");
        }

        // Notion uses JSON body; everyone else uses form-urlencoded
        HttpEntity<?> request;
        if ("notion".equals(providerKey)) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> jsonBody = new HashMap<>();
            jsonBody.put("grant_type", "authorization_code");
            jsonBody.put("code", code);
            jsonBody.put("redirect_uri", redirectUri);
            request = new HttpEntity<>(jsonBody, headers);
        } else {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("redirect_uri", redirectUri);
            if (!useBasicAuth) {
                body.add("client_id", config.getClientId());
                body.add("client_secret", config.getClientSecret());
            }
            // PKCE code_verifier — sent during token exchange
            if (codeVerifier != null) {
                body.add("code_verifier", codeVerifier);
            }
            request = new HttpEntity<>(body, headers);
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    config.getTokenUrl(), HttpMethod.POST, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Token exchange failed for provider " + providerKey);
            }

            return response.getBody();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("OAuth token exchange failed for {}: {}", providerKey, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to exchange OAuth code for " + providerKey + ": " + e.getMessage());
        }
    }

    // ─── PKCE Helpers ──────────────────────────────────────────────

    /**
     * Generates a cryptographically random code_verifier (43-128 chars, URL-safe).
     */
    private String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Computes the S256 code_challenge from the code_verifier.
     */
    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // ─── Account Identity ─────────────────────────────────────────

    /**
     * Fetches the connected user's email and display name from the provider's user
     * API.
     * Returns a map with "email" and "displayName" keys.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> fetchAccountIdentity(String providerKey, Map<String, Object> credentials) {
        String accessToken = credentials.get("accessToken").toString();
        Map<String, String> identity = new HashMap<>();

        if (isGoogleProvider(providerKey)) {
            Map<String, Object> info;
            try {
                info = callUserInfoApi("https://www.googleapis.com/oauth2/v2/userinfo", accessToken, null);
            } catch (Exception firstAttemptError) {
                // Some Google tokens/scopes can fail bearer lookup for userinfo in specific
                // project setups.
                // Retry with access_token query style before giving up.
                try {
                    info = callUserInfoApiWithTokenQuery("https://www.googleapis.com/oauth2/v2/userinfo", accessToken,
                            null);
                } catch (Exception secondAttemptError) {
                    logger.info("Skipping Google identity enrichment for {}: {}", providerKey,
                            secondAttemptError.getMessage());
                    identity.put("email", null);
                    identity.put("displayName", null);
                    return identity;
                }
            }
            identity.put("email", asStr(info.get("email")));
            identity.put("displayName", asStr(info.get("name")));

        } else if (providerKey.startsWith("microsoft-")) {
            Map<String, Object> info = callUserInfoApi(
                    "https://graph.microsoft.com/v1.0/me", accessToken, null);
            identity.put("email", asStr(info.get("mail") != null ? info.get("mail") : info.get("userPrincipalName")));
            identity.put("displayName", asStr(info.get("displayName")));

        } else if ("github".equals(providerKey)) {
            Map<String, Object> info = callUserInfoApi(
                    "https://api.github.com/user", accessToken, null);
            identity.put("email", asStr(info.get("email")));
            identity.put("displayName", asStr(info.get("login")));

        } else if ("discord".equals(providerKey)) {
            Map<String, Object> info = callUserInfoApi(
                    "https://discord.com/api/v10/users/@me", accessToken, null);
            identity.put("email", asStr(info.get("email")));
            String username = asStr(info.get("username"));
            identity.put("displayName", username != null ? username : asStr(info.get("global_name")));

        } else if ("slack".equals(providerKey)) {
            Map<String, Object> info = callUserInfoApi(
                    "https://slack.com/api/auth.test", accessToken, null);
            identity.put("displayName", asStr(info.get("user")));
            identity.put("email", asStr(info.get("team")));

        } else if ("notion".equals(providerKey)) {
            // Notion returns workspace info in the token response itself
            identity.put("displayName", asStr(credentials.get("workspace_name")));
            identity.put("email", null);

        } else if ("reddit".equals(providerKey)) {
            HttpHeaders h = new HttpHeaders();
            h.set("User-Agent", "crescendo:v1.0 (by /u/crescendo-app)");
            Map<String, Object> info = callUserInfoApi(
                    "https://oauth.reddit.com/api/v1/me", accessToken, h);
            identity.put("displayName", asStr(info.get("name")));
            identity.put("email", null);

        } else if ("spotify".equals(providerKey)) {
            Map<String, Object> info = callUserInfoApi(
                    "https://api.spotify.com/v1/me", accessToken, null);
            identity.put("email", asStr(info.get("email")));
            identity.put("displayName", asStr(info.get("display_name")));

        } else if ("linkedin".equals(providerKey)) {
            Map<String, Object> info = callUserInfoApi(
                    "https://api.linkedin.com/v2/userinfo", accessToken, null);
            identity.put("email", asStr(info.get("email")));
            identity.put("displayName", asStr(info.get("name")));

        } else if ("gitlab".equals(providerKey)) {
            Map<String, Object> info = callUserInfoApi(
                    "https://gitlab.com/api/v4/user", accessToken, null);
            identity.put("email", asStr(info.get("email")));
            identity.put("displayName", asStr(info.get("username")));

        } else if ("twitter".equals(providerKey)) {
            Map<String, Object> info = callUserInfoApi(
                    "https://api.twitter.com/2/users/me", accessToken, null);
            Object data = info.get("data");
            if (data instanceof Map d) {
                identity.put("displayName", asStr(d.get("username")));
            }
            identity.put("email", null);

        } else if ("airtable".equals(providerKey)) {
            Map<String, Object> info = callUserInfoApi(
                    "https://api.airtable.com/v0/meta/whoami", accessToken, null);
            identity.put("email", asStr(info.get("email")));
            identity.put("displayName", asStr(info.get("email")));

        } else if ("strava".equals(providerKey)) {
            Map<String, Object> info = callUserInfoApi(
                    "https://www.strava.com/api/v3/athlete", accessToken, null);
            identity.put("displayName", asStr(info.get("firstname")) + " " + asStr(info.get("lastname")));
            identity.put("email", null);

        } else if ("figma".equals(providerKey)) {
            Map<String, Object> info = callUserInfoApi(
                    "https://api.figma.com/v1/me", accessToken, null);
            identity.put("email", asStr(info.get("email")));
            identity.put("displayName", asStr(info.get("handle")));

        } else if ("linear".equals(providerKey)) {
            // Linear uses GraphQL — skip for now, just use generic name
            identity.put("displayName", null);
            identity.put("email", null);
        }

        return identity;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callUserInfoApi(String url, String accessToken, HttpHeaders extraHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (extraHeaders != null)
            headers.addAll(extraHeaders);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        return response.getBody() != null ? response.getBody() : Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callUserInfoApiWithTokenQuery(String url, String accessToken,
            HttpHeaders extraHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (extraHeaders != null)
            headers.addAll(extraHeaders);

        var uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("access_token", accessToken)
                .build(true)
                .toUri();

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, request, Map.class);
        return response.getBody() != null ? response.getBody() : Map.of();
    }

    private String asStr(Object value) {
        return value != null ? value.toString() : null;
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private IntegrationOAuthConfig.ProviderConfig getProviderConfig(String providerKey) {
        if (!oauthConfig.hasProvider(providerKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "OAuth is not configured for provider: " + providerKey);
        }
        return oauthConfig.getProvider(providerKey);
    }

    /**
     * Parses and verifies the HMAC-signed OAuth state parameter.
     * State format: {@code userId:nonce:hmac(userId:nonce, serverSecret)}
     *
     * @param state the state parameter from the OAuth callback
     * @return the verified userId
     * @throws ResponseStatusException 400 if state is missing, malformed, or HMAC invalid
     */
    private UUID parseUserIdFromState(String state) {
        if (state == null || !state.contains(":")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OAuth state");
        }

        // Split into: [userId, nonce, hmac]
        String[] parts = state.split(":", 3);
        if (parts.length != 3) {
            logger.warn("[oauth] Rejected state with wrong part count: {}", parts.length);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OAuth state format");
        }

        String payload = parts[0] + ":" + parts[1];
        String receivedHmac = parts[2];
        String expectedHmac = computeHmac(payload);

        // Constant-time comparison to prevent timing attacks
        if (!MessageDigest.isEqual(
                expectedHmac.getBytes(StandardCharsets.UTF_8),
                receivedHmac.getBytes(StandardCharsets.UTF_8))) {
            logger.warn("[oauth] Rejected state with invalid HMAC signature");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OAuth state signature");
        }

        try {
            return UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userId in OAuth state");
        }
    }

    /**
     * Computes HMAC-SHA256 of the given payload using the server's crypto key.
     * Returns a hex-encoded string.
     */
    private String computeHmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacKeyBytes, HMAC_ALGORITHM));
            byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmac);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    /**
     * Resolves the Crescendo appKey from the OAuth provider key.
     * Most provider keys map 1:1, but Google services are separate apps.
     */
    private String resolveAppKey(String providerKey) {
        return switch (providerKey) {
            case "google-gmail", "gmail" -> "gmail";
            case "google-sheets" -> "google-sheets";
            case "google-drive" -> "google-drive";
            case "google-calendar" -> "google-calendar";
            case "google-forms" -> "google-forms";
            case "google-tasks" -> "google-tasks";
            case "google-slides" -> "google-slides";
            case "google-docs" -> "google-docs";
            default -> providerKey;
        };
    }

    private boolean isGoogleProvider(String providerKey) {
        return "gmail".equals(providerKey)
                || providerKey.startsWith("google-");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
