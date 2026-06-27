package com.crescendo.apps.spotify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exchanges user-provided Spotify {@code clientId} + {@code clientSecret}
 * for an access token using the <b>Client Credentials</b> flow.
 *
 * <p>Tokens are cached in-memory per clientId and automatically refreshed
 * when they expire (Spotify tokens are valid for 1 hour).
 *
 * <p>This is used for normal (non-admin) users who provide their own
 * Spotify Developer App credentials.  Admin users still use the
 * server-side OAuth2 flow configured in {@code application.properties}.
 */
@Component
public class SpotifySupport {

    private static final Logger logger = LoggerFactory.getLogger(SpotifySupport.class);
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    /** Simple in-memory cache: clientId → (token, expiresAt). */
    private final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    /**
     * Returns a valid Spotify access token for the given client credentials.
     * Uses the Client Credentials flow (no user interaction needed).
     *
     * @param clientId     Spotify Developer App client ID
     * @param clientSecret Spotify Developer App client secret
     * @return a bearer access token
     * @throws SpotifyAuthException if the token exchange fails
     */
    public String getAccessToken(String clientId, String clientSecret) {
        // Check cache first
        CachedToken cached = tokenCache.get(clientId);
        if (cached != null && cached.isValid()) {
            return cached.token;
        }

        // Exchange credentials for token
        try {
            String basicAuth = java.util.Base64.getEncoder().encodeToString(
                    (clientId + ":" + clientSecret).getBytes(java.nio.charset.StandardCharsets.UTF_8));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = RestClient.create()
                    .post()
                    .uri(TOKEN_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new SpotifyAuthException("Spotify returned no access_token — verify your Client ID and Secret");
            }

            String token = response.get("access_token").toString();
            long expiresIn = response.containsKey("expires_in")
                    ? ((Number) response.get("expires_in")).longValue()
                    : 3600L;

            // Cache with 60s safety margin
            tokenCache.put(clientId, new CachedToken(token, Instant.now().plusSeconds(expiresIn - 60)));

            logger.debug("[spotify] Client Credentials token obtained for clientId={}", clientId.substring(0, 8) + "...");
            return token;

        } catch (SpotifyAuthException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[spotify] Client Credentials flow failed for clientId={}: {}", clientId.substring(0, 8) + "...", e.getMessage());
            throw new SpotifyAuthException("Spotify authentication failed: " + e.getMessage());
        }
    }

    /**
     * Resolves an access token from user credentials.
     * Supports both:
     * <ul>
     *   <li>OAuth2 connections (admin): credentials contain {@code accessToken} directly</li>
     *   <li>APIKEY connections (normal users): credentials contain {@code clientId} + {@code clientSecret}</li>
     * </ul>
     */
    public String resolveAccessToken(Map<String, Object> credentials) {
        if (credentials == null) {
            throw new SpotifyAuthException("No credentials provided for Spotify");
        }

        // OAuth2 path (admin users): token is already present
        String existingToken = asStr(credentials.get("accessToken"));
        if (existingToken != null && !existingToken.isBlank()) {
            return existingToken;
        }

        // APIKEY path (normal users): exchange clientId + clientSecret
        String clientId = asStr(credentials.get("clientId"));
        String clientSecret = asStr(credentials.get("clientSecret"));

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new SpotifyAuthException(
                    "Spotify credentials must contain either 'accessToken' (OAuth) or 'clientId' + 'clientSecret' (API Key)");
        }

        return getAccessToken(clientId, clientSecret);
    }

    private String asStr(Object value) {
        return value != null ? value.toString() : null;
    }

    /** Cached token with expiry. */
    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    /** Thrown when Spotify authentication fails. */
    public static class SpotifyAuthException extends RuntimeException {
        public SpotifyAuthException(String message) {
            super(message);
        }
    }
}
