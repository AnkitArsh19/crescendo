package com.crescendo.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Stateless JWT authentication filter:
 * - Extracts access token from Authorization: Bearer header or access_token query (fallback)
 * - Validates signature & expiry
 * - Loads user details and populates SecurityContext
 * Refresh logic is handled by /auth/refresh endpoint (not in filter) to keep concerns separated.
 */
@Component
public class JWTFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JWTFilter(JWTService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip if already authenticated — another filter earlier in the chain may have set this.
        // Avoids redundant DB calls on the same request.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
    if (token != null) {
            try {
                String username = jwtService.extractUserName(token);
                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.validateAccessToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (ExpiredJwtException ex) {
                // Access token expired — do NOT return 401 here.
                // Let the request fall through unauthenticated; SecurityConfig will deny it if the
                // endpoint requires auth. The client should detect 401 and call /auth/refresh.
            } catch (Exception ex) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or malformed token\"}");
        return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /// Extracts the raw JWT from the request.
    /// Primary source: Authorization header ("Bearer <token>") — standard for REST APIs.
    /// Fallback: access_token query parameter — used for WebSocket connections and browser-initiated
    /// file downloads where setting a custom header is not possible.
    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        String queryToken = request.getParameter("access_token");
        return (queryToken != null && !queryToken.isBlank()) ? queryToken : null;
    }
}
