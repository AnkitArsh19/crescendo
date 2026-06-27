package com.crescendo.publicapi.oauth;

import com.crescendo.security.access.AccessControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@RestController
public class OAuthSessionController {
    private static final int SESSION_TTL_SECONDS = 10 * 60;

    private final HttpSessionSecurityContextRepository securityContextRepository;
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
    private final String frontendUrl;
    private final AccessControlService accessControl;

    public OAuthSessionController(
            HttpSessionSecurityContextRepository securityContextRepository,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            AccessControlService accessControl) {
        this.securityContextRepository = securityContextRepository;
        this.frontendUrl = frontendUrl;
        this.accessControl = accessControl;
    }

    /**
     * Browser entry point used when an OAuth authorization request has no Crescendo session.
     */
    @GetMapping("/oauth/session-required")
    public void sessionRequired(HttpServletResponse response) throws IOException {
        String location = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/oauth/authorize")
                .build()
                .toUriString();
        response.sendRedirect(location);
    }

    /**
     * Exchanges the first-party JWT-authenticated request for a short-lived browser session
     * used only by the authorization and consent endpoints.
     */
    @PostMapping("/oauth/session")
    public SessionResponse createAuthorizationSession(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        accessControl.requireFullAccess();

        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No pending OAuth authorization request"
            );
        }
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null || !isAuthorizationRequest(savedRequest.getRedirectUrl(), request)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No valid OAuth authorization request is pending"
            );
        }

        request.changeSessionId();
        session.setMaxInactiveInterval(SESSION_TTL_SECONDS);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextRepository.saveContext(context, request, response);

        return new SessionResponse(savedRequest.getRedirectUrl(), SESSION_TTL_SECONDS);
    }

    private boolean isAuthorizationRequest(String redirectUrl, HttpServletRequest request) {
        if (redirectUrl == null) {
            return false;
        }
        String expectedPrefix = request.getScheme() + "://" + request.getServerName();
        if (!isDefaultPort(request)) {
            expectedPrefix += ":" + request.getServerPort();
        }
        return redirectUrl.startsWith(expectedPrefix + "/oauth2/authorize");
    }

    private boolean isDefaultPort(HttpServletRequest request) {
        return ("https".equals(request.getScheme()) && request.getServerPort() == 443)
                || ("http".equals(request.getScheme()) && request.getServerPort() == 80);
    }

    public record SessionResponse(String authorizationUrl, int expiresInSeconds) {
    }
}
