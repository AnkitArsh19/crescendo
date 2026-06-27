package com.crescendo.security;

import com.crescendo.publicapi.oauth.DeveloperApplicationRepository;
import com.crescendo.publicapi.oauth.OAuthAccessTokenUsageLog;
import com.crescendo.publicapi.oauth.OAuthAccessTokenUsageLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * Resolves opaque OAuth access tokens issued by Crescendo for public API requests.
 */
@Component
public class OAuthAccessTokenAuthenticationFilter extends OncePerRequestFilter {
    public static final String OAUTH_CLIENT_ID_ATTRIBUTE = "crescendo.oauthClientId";

    private final OAuth2AuthorizationService authorizations;
    private final DeveloperApplicationRepository applications;
    private final AppUserDetailsService users;
    private final StringRedisTemplate redisTemplate;
    private final OAuthAccessTokenUsageLogRepository usageLogs;

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    public OAuthAccessTokenAuthenticationFilter(
            OAuth2AuthorizationService authorizations,
            DeveloperApplicationRepository applications,
            AppUserDetailsService users,
            StringRedisTemplate redisTemplate,
            OAuthAccessTokenUsageLogRepository usageLogs) {
        this.authorizations = authorizations;
        this.applications = applications;
        this.users = users;
        this.redisTemplate = redisTemplate;
        this.usageLogs = usageLogs;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!isPublicApiPath(request)
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = bearerToken(request);
        if (token == null || token.startsWith("re_")) {
            filterChain.doFilter(request, response);
            return;
        }

        var authorization = authorizations.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null
                || authorization.getAccessToken() == null
                || !authorization.getAccessToken().isActive()) {
            filterChain.doFilter(request, response);
            return;
        }
        var application = applications
                .findByIdAndActiveTrue(authorization.getRegisteredClientId())
                .orElse(null);
        if (application == null) {
            filterChain.doFilter(request, response);
            return;
        }

        AppUserDetails user = (AppUserDetails) users.loadUserByUsername(
                authorization.getPrincipalName());
        if (isRateLimited(application.getId(), user.getId(), application.getRateLimitPerMinute())) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"OAuth application rate limit exceeded\"}"
            );
            return;
        }
        PublicApiPrincipal principal = PublicApiPrincipal.oauth2(
                user,
                authorization.getId(),
                application.getClientId(),
                authorization.getAuthorizedScopes()
        );
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(OAUTH_CLIENT_ID_ATTRIBUTE, application.getClientId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            audit(request, response, application.getId(), authorization.getId(), user.getId());
        }
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }

    private boolean isPublicApiPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/v1/");
    }

    private boolean isRateLimited(
            String applicationId, UUID userId, int rateLimitPerMinute) {
        String key = "crescendo:ratelimit:oauth:" + applicationId + ":" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        return count != null && count > Math.max(1, rateLimitPerMinute);
    }

    private void audit(
            HttpServletRequest request,
            HttpServletResponse response,
            String applicationId,
            String authorizationId,
            UUID userId) {
        try {
            usageLogs.save(new OAuthAccessTokenUsageLog(
                    UUID.randomUUID(),
                    applicationId,
                    authorizationId,
                    userId,
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
}
