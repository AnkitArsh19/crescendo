package com.crescendo.security;

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

    public void setRefreshToken(HttpServletResponse response, String token, long ttlMillis, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .path("/auth")
                .sameSite("Strict")
                .maxAge(Duration.ofMillis(ttlMillis))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clear(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path("/auth")
                .sameSite("Strict")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
