package com.crescendo.security;

import com.crescendo.emailservice.apikey.key_command.ApiKey_command;
import com.crescendo.emailservice.apikey.key_command.ApiKey_commandRepository;
import com.crescendo.emailservice.apikey.key_command.ApiKey_commandService;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_command.user_credential.UserCredentialRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Authenticates requests carrying an API key in the Authorization header.
 *
 * API keys use the format: Authorization: Bearer re_<base64url>
 * The "re_" prefix distinguishes them from JWT tokens so the filter can decide
 * quickly whether to handle the request or pass it to JWTFilter.
 *
 * Flow:
 *   1. Check for "Bearer re_" prefix — skip if absent
 *   2. Extract lookup prefix (first 8 chars) and find the key record
 *   3. Verify SHA-256 hash matches the stored hash
 *   4. Check rate limit via Redis INCR
 *   5. Load the key owner's User_command and build AppUserDetails
 *   6. Set SecurityContext — JWTFilter will skip because auth is already set
 *   7. Update lastUsedAt for usage tracking
 *
 * This filter MUST be registered before JWTFilter in the security chain.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "re_";
    private static final int LOOKUP_PREFIX_LENGTH = 8;
    private static final long RATE_LIMIT_MAX = 100;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final String RATE_LIMIT_KEY_PREFIX = "crescendo:ratelimit:apikey:";
    public static final String API_KEY_ID_ATTRIBUTE = "crescendo.apiKeyId";

    private final ApiKey_commandRepository apiKeyRepo;
    private final User_commandRepository userRepo;
    private final UserCredentialRepository credentialRepo;
    private final StringRedisTemplate redisTemplate;

    public ApiKeyAuthenticationFilter(ApiKey_commandRepository apiKeyRepo,
                                      User_commandRepository userRepo,
                                      UserCredentialRepository credentialRepo,
                                      StringRedisTemplate redisTemplate) {
        this.apiKeyRepo = apiKeyRepo;
        this.userRepo = userRepo;
        this.credentialRepo = credentialRepo;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Only handle Bearer tokens with the re_ prefix — everything else falls through to JWTFilter
        if (authHeader == null || !authHeader.startsWith("Bearer " + KEY_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = authHeader.substring(7); // strip "Bearer "

        if (rawKey.length() < LOOKUP_PREFIX_LENGTH) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key format");
            return;
        }

        // Look up by prefix for fast O(1) index scan
        String prefix = rawKey.substring(0, LOOKUP_PREFIX_LENGTH);
        ApiKey_command apiKey = apiKeyRepo.findByPrefix(prefix).orElse(null);

        if (apiKey == null || apiKey.getRevokedAt() != null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or revoked API key");
            return;
        }

        // Verify the full key hash — prefix alone is not sufficient for authentication
        String providedHash = ApiKey_commandService.sha256Hex(rawKey);
        if (!providedHash.equals(apiKey.getHashedKey())) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return;
        }

        // Rate limit per API key
        if (isRateLimited(apiKey.getId().toString())) {
            sendError(response, 429, "Rate limit exceeded");
            return;
        }

        // Load the key owner to build a full security principal
        User_command user = userRepo.findById(apiKey.getUserId()).orElse(null);
        if (user == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "API key owner not found");
            return;
        }

        AppUserDetails userDetails = AppUserDetails.from(user, credentialRepo.findByUser_Id(user.getId()));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Expose the API key ID as a request attribute so downstream controllers
        // (e.g., SendEmailController) can track which key was used.
        request.setAttribute(API_KEY_ID_ATTRIBUTE, apiKey.getId());

        // Track usage (best-effort — no transaction required for a single UPDATE)
        apiKey.setLastUsedAt(Instant.now());
        apiKeyRepo.save(apiKey);

        filterChain.doFilter(request, response);
    }

    /// Sliding-window rate limiter using Redis INCR + EXPIRE.
    /// Returns true if the key has exceeded RATE_LIMIT_MAX requests within the current window.
    private boolean isRateLimited(String apiKeyId) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKeyId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, RATE_LIMIT_WINDOW);
        }
        return count != null && count > RATE_LIMIT_MAX;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String errorLabel = status == 429 ? "Too Many Requests" : "Unauthorized";
        response.getWriter().write(
                "{\"status\":" + status + ",\"error\":\"" + errorLabel + "\",\"message\":\"" + message + "\"}");
    }
}
