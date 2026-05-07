package com.crescendo.security.error;

import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles requests where the user IS authenticated but lacks the required role/authority
 * for the endpoint they are trying to access. Returns a JSON 403 Forbidden response.
 * Wired into SecurityConfig via .accessDeniedHandler(...).
 * Distinction from AuthenticationEntryPoint: 401 = who are you?, 403 = I know who you are but you can't do this.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String,Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("message", accessDeniedException.getMessage());
        body.put("path", request.getRequestURI());
        mapper.writeValue(response.getOutputStream(), body);
    }
}
