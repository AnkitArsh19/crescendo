package com.crescendo.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Two-layered rate limiter for sensitive authentication endpoints.
 *
 * <p>Inspired by n8n's production rate-limit service which uses IP-based (Layer 1)
 * followed by keyed (Layer 2) limiting to balance security and usability:
 *
 * <ul>
 *   <li><b>Layer 1 — IP-based:</b> Blocks volumetric/bot attacks at the network level.
 *       Applied to ALL protected auth paths regardless of request body content.
 *       Default: 20 requests per minute per IP.</li>
 *   <li><b>Layer 2 — Identity-keyed:</b> Blocks targeted dictionary/credential-stuffing
 *       attacks against a specific email/username. Extracts the email from the request body
 *       JSON and applies a stricter per-identity limit. Applied to login only.
 *       Default: 10 requests per minute per email.</li>
 * </ul>
 *
 * <p>This filter runs BEFORE Spring Security's authentication chain. A 429 response
 * is returned immediately without touching the database or running credential checks.
 *
 * <p>Protected paths (configurable):
 * <ul>
 *   <li>POST /auth/login</li>
 *   <li>POST /auth/register</li>
 *   <li>POST /auth/forgot-password</li>
 *   <li>POST /auth/reset-password</li>
 * </ul>
 */
@Component
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    // --- Namespaces for Redis keys ---
    private static final String NS_IP   = "auth:ip";
    private static final String NS_EMAIL = "auth:email";

    // --- Paths this filter protects ---
    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/webauthn/login/start",
            "/auth/webauthn/login/finish",
            "/auth/webauthn/recovery/magic-link",
            "/auth/webauthn/recovery/register/start",
            "/auth/webauthn/recovery/register/finish",
            "/auth/webauthn/passwordless/start",
            "/auth/webauthn/passwordless/verify",
            "/auth/webauthn/passwordless/finish"
    );

    // --- Layer 1: IP limits ---
    @Value("${app.security.rate-limit.auth.ip.max:20}")
    private int ipMaxRequests;

    @Value("${app.security.rate-limit.auth.ip.window-minutes:1}")
    private int ipWindowMinutes;

    // --- Layer 2: Email/identity limits (login only) ---
    @Value("${app.security.rate-limit.auth.email.max:10}")
    private int emailMaxRequests;

    @Value("${app.security.rate-limit.auth.email.window-minutes:1}")
    private int emailWindowMinutes;

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    private final RateLimitingService rateLimiter;
    private final ObjectMapper objectMapper;

    public AuthRateLimitingFilter(RateLimitingService rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only apply to protected paths
        if (!PROTECTED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // The identity limiter needs to inspect JSON. Replay the cached body to
        // Spring MVC afterwards so controllers can still deserialize it.
        HttpServletRequest cachedRequest = new CachedBodyRequest(request);
        String clientIp = resolveClientIp(cachedRequest);

        // ── LAYER 1: IP-based rate limit ──────────────────────────────────────
        if (rateLimiter.isRateLimited(NS_IP, clientIp, ipMaxRequests, Duration.ofMinutes(ipWindowMinutes))) {
            sendTooManyRequests(response,
                    "Too many requests from your IP address. Please wait before retrying.");
            return;
        }

        // ── LAYER 2: Identity-keyed rate limit (login only) ───────────────────
        // We extract the email from the cached request body. This only applies to
        // /auth/login because that's where credential stuffing attacks happen.
        if (isIdentityLimitedPath(path) && "POST".equalsIgnoreCase(cachedRequest.getMethod())) {
            String email = extractEmailFromBody(cachedRequest);
            if (email != null && !email.isBlank()) {
                if (rateLimiter.isRateLimited(NS_EMAIL, email.toLowerCase(),
                        emailMaxRequests, Duration.ofMinutes(emailWindowMinutes))) {
                    sendTooManyRequests(response,
                            "Too many login attempts for this account. Please wait before retrying.");
                    return;
                }
            }
        }

        filterChain.doFilter(cachedRequest, response);
    }

    /**
     * Resolves the client IP, honoring X-Forwarded-For only if the app is configured
     * to trust forwarded headers (i.e., running behind a trusted reverse proxy).
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // X-Forwarded-For may contain a chain: "client, proxy1, proxy2"
                // The leftmost IP is the original client.
                String first = forwarded.split(",")[0].trim();
                if (!first.isEmpty()) return first;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Attempts to extract the "email" field from the JSON request body.
     * Returns null if the body is not JSON, is empty, or does not contain an email field.
     *
     * <p>We use a cached wrapper (ContentCachingRequestWrapper) injected by Spring's
     * filter chain so the body can be read without consuming the stream.
     */
    @SuppressWarnings("unchecked")
    private String extractEmailFromBody(HttpServletRequest request) {
        try {
            // Only proceed if there is content to read
            if (request.getContentLength() <= 0 && request.getInputStream().available() == 0) {
                return null;
            }
            Map<String, Object> body = objectMapper.readValue(request.getInputStream(), Map.class);
            Object email = body.get("email");
            return email instanceof String s ? s : null;
        } catch (Exception ignored) {
            // Malformed JSON or empty body — skip Layer 2 gracefully
            return null;
        }
    }

    private boolean isIdentityLimitedPath(String path) {
        return "/auth/login".equals(path)
                || "/auth/webauthn/login/start".equals(path)
                || "/auth/webauthn/passwordless/start".equals(path)
                || "/auth/webauthn/recovery/magic-link".equals(path);
    }

    /** Replays the JSON body after the rate limiter reads it. */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { }
            };
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private void sendTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"" + message + "\"}"
        );
    }
}
