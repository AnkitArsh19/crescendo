package com.crescendo.publicapi;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all public API endpoints.
 * Returns a standardized JSON shape: { "type": "...", "message": "...",
 * "status": 4xx }.
 * Ordered HIGHEST_PRECEDENCE so it runs before the generic
 * RestExceptionHandler.
 *
 * Covers:
 * - com.crescendo.publicapi (Workflow, Run, Connection, App catalog)
 * - com.crescendo.publicapi.email (Domain, Audience, Suppression)
 * - com.crescendo.emailservice.email_send
 * - com.crescendo.emailservice.email_log
 * - com.crescendo.emailservice.outboundwebhook
 * - com.crescendo.emailservice.customevent
 */
@RestControllerAdvice(basePackages = {
        "com.crescendo.publicapi",
        "com.crescendo.emailservice.email_send",
        "com.crescendo.emailservice.email_log",
        "com.crescendo.emailservice.outboundwebhook",
        "com.crescendo.emailservice.customevent"
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicApiExceptionAdvice {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return buildResponse(status, toType(status), reason);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "invalid_request_error", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, "conflict_error", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "api_error", "Internal Server Error");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String type, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", type);
        body.put("message", message);
        body.put("status", status.value());
        return new ResponseEntity<>(body, status);
    }

    private String toType(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> "invalid_request_error";
            case 401 -> "unauthorized_error";
            case 403 -> "forbidden_error";
            case 404 -> "not_found_error";
            case 409 -> "conflict_error";
            case 429 -> "rate_limit_error";
            default -> "api_error";
        };
    }
}
