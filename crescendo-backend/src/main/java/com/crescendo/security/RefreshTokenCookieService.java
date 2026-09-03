package com.crescendo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Helper to set and clear the refresh token as an HttpOnly, Secure cookie.
 * The refresh token value itself is opaque to the client JS (HttpOnly=true).
 */
@Component
public class RefreshTokenCookieService {

    private static final String COOKIE_NAME = "refresh_token";

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    /// Sets the refresh token as a cookie with full security attributes.
    /// httpOnly=true  — JavaScript cannot read this cookie, preventing XSS token theft.
    /// secure=true    — cookie sent only over HTTPS (set false in local dev where HTTP is used).
    /// path="/auth"   — cookie is only sent to /auth/* endpoints, not every API call.
    ///                  This minimizes exposure; the browser won't attach it to /workflows requests.
    /// sameSite       — Lax allows same-site cross-subdomain calls (app.crescendo.run -> api.crescendo.run) while blocking CSRF.
    public void setRefreshToken(HttpServletResponse response, String token, long ttlMillis, boolean secure) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .path("/auth")
                .sameSite(sameSite)
                .maxAge(Duration.ofMillis(ttlMillis));

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain.trim());
        }

        ResponseCookie cookie = builder.build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /// Clears the cookie on logout by setting the same cookie with maxAge=0 (Duration.ZERO).
    /// The browser immediately discards a cookie with maxAge=0.
    /// All other attributes must exactly match the original cookie or the browser won't clear it.
    public void clear(HttpServletResponse response, boolean secure) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path("/auth")
                .sameSite(sameSite)
                .maxAge(Duration.ZERO);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain.trim());
        }

        ResponseCookie cookie = builder.build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
