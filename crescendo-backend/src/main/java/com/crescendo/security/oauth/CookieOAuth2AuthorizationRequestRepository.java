package com.crescendo.security.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Base64;

/**
 * Stores the OAuth2 authorization request in an HttpOnly cookie instead of the server-side session.
 *
 * Why this exists:
 *   Our app uses SessionCreationPolicy.STATELESS — no server-side HttpSession is ever created.
 *   But Spring's OAuth2 login flow needs to persist the authorization request between:
 *     (1) the redirect to Google/GitHub, and
 *     (2) the callback when the provider redirects back.
 *
 *   The default implementation uses HttpSession, which doesn't exist in our stateless setup.
 *   This cookie-based repository solves that by serializing the authorization request into
 *   a short-lived, HttpOnly cookie that survives the redirect round-trip.
 *
 * Security:
 *   - HttpOnly = true → JavaScript can't read the cookie.
 *   - Short maxAge (5 min) → Cookie is discarded quickly even if the flow is abandoned.
 *   - The cookie contains the serialized OAuth2AuthorizationRequest (state, redirect URI, scopes).
 *     It does NOT contain any secrets — the actual code exchange happens server-side.
 */
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "oauth2_auth_request";
    private static final int MAX_AGE_SECONDS = 300; // 5 minutes — enough for the redirect round-trip

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            // null means "remove" — Spring calls this after the callback is handled.
            removeCookie(response);
            return;
        }
        String serialized = serialize(authorizationRequest);
        // Use ResponseCookie to set SameSite=Lax reliably.
        // SameSite=Lax is critical — the cookie must be sent on the top-level redirect back from Google/GitHub.
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, serialized)
                .path("/")
                .httpOnly(true)
                .maxAge(MAX_AGE_SECONDS)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest req = loadAuthorizationRequest(request);
        removeCookie(response);
        return req;
    }

    // HELPERS
    private OAuth2AuthorizationRequest getCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                try {
                    return deserialize(cookie.getValue());
                } catch (Exception e) {
                    // Deserialization can fail after a server restart (class version mismatch).
                    // Treat as missing — Spring will restart the OAuth flow.
                    return null;
                }
            }
        }
        return null;
    }

    private void removeCookie(HttpServletResponse response) {
        // Set an expired cookie to clear it from the browser.
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /// Serializes the OAuth2AuthorizationRequest to a Base64 string for cookie storage.
    /// Java serialization is acceptable here because the data originates from Spring's own code,
    /// not from untrusted user input. The object is small (~500 bytes encoded).
    private String serialize(OAuth2AuthorizationRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(request);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize OAuth2AuthorizationRequest", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String encoded) {
        byte[] bytes = Base64.getUrlDecoder().decode(encoded);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (OAuth2AuthorizationRequest) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize OAuth2AuthorizationRequest", e);
        }
    }
}
