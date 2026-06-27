package com.crescendo.security;

import com.crescendo.emailservice.apikey.key_command.ApiKey_commandService;
import com.crescendo.publicapi.oauth.OAuthConsumedRefreshToken;
import com.crescendo.publicapi.oauth.OAuthConsumedRefreshTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Retains hashes of rotated refresh tokens so replay revokes the complete authorization.
 */
@Component
public class OAuthRefreshTokenReuseDetectionFilter extends OncePerRequestFilter {
    private final OAuth2AuthorizationService authorizations;
    private final OAuthConsumedRefreshTokenRepository consumedTokens;

    public OAuthRefreshTokenReuseDetectionFilter(
            OAuth2AuthorizationService authorizations,
            OAuthConsumedRefreshTokenRepository consumedTokens) {
        this.authorizations = authorizations;
        this.consumedTokens = consumedTokens;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String refreshToken = refreshToken(request);
        if (refreshToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String tokenHash = ApiKey_commandService.sha256Hex(refreshToken);
        var consumed = consumedTokens.findById(tokenHash);
        if (consumed.isPresent()) {
            revokeAuthorization(consumed.get().getAuthorizationId());
            oauthError(response, "Refresh token reuse detected; authorization revoked");
            return;
        }

        OAuth2Authorization authorization =
                authorizations.findByToken(refreshToken, OAuth2TokenType.REFRESH_TOKEN);
        filterChain.doFilter(request, response);

        if (response.getStatus() >= 200
                && response.getStatus() < 300
                && authorization != null
                && authorization.getRefreshToken() != null) {
            Instant expiresAt = authorization.getRefreshToken().getToken().getExpiresAt();
            if (expiresAt != null) {
                try {
                    consumedTokens.saveAndFlush(new OAuthConsumedRefreshToken(
                            tokenHash,
                            authorization.getId(),
                            authorization.getRegisteredClientId(),
                            authorization.getPrincipalName(),
                            expiresAt
                    ));
                } catch (DataIntegrityViolationException duplicateUse) {
                    revokeAuthorization(authorization.getId());
                }
            }
        }
    }

    private String refreshToken(HttpServletRequest request) {
        if (!"/oauth2/token".equals(request.getRequestURI())
                || !"refresh_token".equals(request.getParameter("grant_type"))) {
            return null;
        }
        String token = request.getParameter("refresh_token");
        return token == null || token.isBlank() ? null : token;
    }

    private void revokeAuthorization(String authorizationId) {
        OAuth2Authorization authorization = authorizations.findById(authorizationId);
        if (authorization != null) {
            authorizations.remove(authorization);
        }
        consumedTokens.deleteByAuthorizationId(authorizationId);
    }

    private void oauthError(HttpServletResponse response, String description) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"error\":\"invalid_grant\",\"error_description\":\"" + description + "\"}"
        );
    }
}
