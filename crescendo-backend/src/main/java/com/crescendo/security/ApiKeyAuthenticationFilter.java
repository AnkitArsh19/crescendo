package com.crescendo.security;

import com.crescendo.emailservice.apikey.key_command.ApiKey_command;
import com.crescendo.emailservice.apikey.key_command.ApiKey_commandRepository;
import com.crescendo.emailservice.apikey.key_command.ApiKey_commandService;
import com.crescendo.emailservice.apikey.key_query.ApiKey_queryRepository;
import com.crescendo.publicapi.audit.ApiKeyUsageLog;
import com.crescendo.publicapi.audit.ApiKeyUsageLogRepository;
import com.crescendo.publicapi.PublicApiScopes;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_command.user_credential.UserCredentialRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;

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
    private static final String LIVE_KEY_PREFIX = "re_live_";
    private static final int LEGACY_LOOKUP_PREFIX_LENGTH = 8;
    private static final int LIVE_LOOKUP_PREFIX_LENGTH = 16;
    private static final String RATE_LIMIT_NAMESPACE = "apikey";
    public static final String API_KEY_ID_ATTRIBUTE = "crescendo.apiKeyId";

    private final ApiKey_commandRepository apiKeyRepo;
    private final User_commandRepository userRepo;
    private final UserCredentialRepository credentialRepo;
    private final RateLimitingService rateLimitingService;
    private final ApiKeyUsageLogRepository usageLogRepo;
    private final ApiKey_queryRepository apiKeyQueryRepo;

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    public ApiKeyAuthenticationFilter(ApiKey_commandRepository apiKeyRepo,
                                      User_commandRepository userRepo,
                                      UserCredentialRepository credentialRepo,
                                      RateLimitingService rateLimitingService,
                                      ApiKeyUsageLogRepository usageLogRepo,
                                      ApiKey_queryRepository apiKeyQueryRepo) {
        this.apiKeyRepo = apiKeyRepo;
        this.userRepo = userRepo;
        this.credentialRepo = credentialRepo;
        this.rateLimitingService = rateLimitingService;
        this.usageLogRepo = usageLogRepo;
        this.apiKeyQueryRepo = apiKeyQueryRepo;
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

        if (!isPublicApiPath(request)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN,
                    "API keys can only access /api/v1 endpoints");
            return;
        }

        int lookupPrefixLength = rawKey.startsWith(LIVE_KEY_PREFIX)
                ? LIVE_LOOKUP_PREFIX_LENGTH
                : LEGACY_LOOKUP_PREFIX_LENGTH;
        if (rawKey.length() < lookupPrefixLength) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key format");
            return;
        }

        // Look up by prefix for fast O(1) index scan
        String prefix = rawKey.substring(0, lookupPrefixLength);
        ApiKey_command apiKey = apiKeyRepo.findByPrefix(prefix).orElse(null);

        if (apiKey == null || !apiKey.isUsableAt(Instant.now())) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or revoked API key");
            return;
        }

        // Verify the full key hash — prefix alone is not sufficient for authentication
        if (!ApiKey_commandService.securelyMatches(rawKey, apiKey.getHashedKey())) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return;
        }

        // Rate limit per API key via the shared RateLimitingService
        if (rateLimitingService.isRateLimited(RATE_LIMIT_NAMESPACE, apiKey.getId().toString(),
                apiKey.getRateLimitPerMinute(), Duration.ofMinutes(1))) {
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

        PublicApiPrincipal principal = PublicApiPrincipal.apiKey(
                userDetails,
                apiKey.getId(),
                PublicApiScopes.parse(apiKey.getScopes())
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Expose the API key ID as a request attribute so downstream controllers
        // (e.g., SendEmailController) can track which key was used.
        request.setAttribute(API_KEY_ID_ATTRIBUTE, apiKey.getId());

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Track usage after the controller runs so audit records include the final status code.
            apiKey.setLastUsedAt(Instant.now());
            apiKeyRepo.save(apiKey);
            apiKeyQueryRepo.findById(apiKey.getId()).ifPresent(query -> {
                query.setLastUsedAt(apiKey.getLastUsedAt());
                apiKeyQueryRepo.save(query);
            });
            audit(request, response, apiKey);
        }
    }

    private boolean isPublicApiPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/v1/");
    }

    private void audit(HttpServletRequest request, HttpServletResponse response, ApiKey_command apiKey) {
        try {
            usageLogRepo.save(new ApiKeyUsageLog(
                    UUID.randomUUID(),
                    apiKey.getId(),
                    apiKey.getUserId(),
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    clientIp(request),
                    trim(request.getHeader("User-Agent"), 1000)
            ));
        } catch (Exception ignored) {
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return trim(forwarded.split(",")[0].trim(), 100);
            }
        }
        return trim(request.getRemoteAddr(), 100);
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String errorLabel = switch (status) {
            case 403 -> "Forbidden";
            case 429 -> "Too Many Requests";
            default -> "Unauthorized";
        };
        response.getWriter().write(
                "{\"status\":" + status + ",\"error\":\"" + errorLabel + "\",\"message\":\"" + message + "\"}");
    }
}
