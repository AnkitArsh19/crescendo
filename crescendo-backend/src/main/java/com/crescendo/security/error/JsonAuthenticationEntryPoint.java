package com.crescendo.security.error;

import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles requests that reach a protected endpoint without any authentication at all
 * (i.e., missing or invalid Authorization header). Returns a JSON 401 Unauthorized response
 * instead of Spring Security's default redirect to a login page (which is meaningless for REST APIs).
 * Wired into SecurityConfig via .authenticationEntryPoint(...).
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String,Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getRequestURI());
        mapper.writeValue(response.getOutputStream(), body);
    }
}
