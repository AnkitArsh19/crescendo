package com.crescendo.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles OAuth2 login failures (user denied consent, provider error, token exchange failure, etc.).
 * Redirects to the frontend login page with an error query parameter so the UI can show a toast.
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.warn("OAuth2 login failed: {}", exception.getMessage());

        // Encode the error message so it's safe in a URL query string.
        String encodedError = URLEncoder.encode(
                exception.getMessage() != null ? exception.getMessage() : "OAuth login failed",
                StandardCharsets.UTF_8
        );

        // Redirect back to the frontend login page with an error parameter.
        // The frontend reads ?error= and displays it as a toast notification.
        response.sendRedirect(frontendUrl + "/login?error=" + encodedError);
    }
}
