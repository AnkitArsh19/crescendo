package com.crescendo.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST controllers.
 * {@code @RestControllerAdvice} catches exceptions thrown anywhere in the controller layer
 * and returns a structured JSON error response instead of Spring's default HTML error page.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} to override handling of built-in
 * Spring MVC exceptions.</p>
 *
 * <p><strong>Why ResponseStatusException is handled explicitly:</strong>
 * Spring Boot 3+/4 returns RFC 9457 ProblemDetail format for {@code ResponseStatusException},
 * using a {@code detail} field. Our frontend universally reads {@code message}. Without this
 * handler, every {@code ResponseStatusException} message would be invisible to the client —
 * it would fall back to generic text like "Activation failed" instead of showing
 * "Step 2: select a connected account."</p>
 */
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles {@link ResponseStatusException} thrown by service/controller code.
     *
     * <p>Without this handler, Spring's built-in {@code ResponseEntityExceptionHandler}
     * returns RFC 9457 ProblemDetail with a {@code detail} field. Our frontend reads
     * {@code err.response?.data?.message}, which would be {@code undefined} for
     * ProblemDetail responses. This handler returns our standard {@code { message: "..." }}
     * format so all error messages reach the client consistently.</p>
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatus(ResponseStatusException ex, WebRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return build(status, reason, req);
    }

    /**
     * Handles validation failures from @Valid / @Validated on request bodies.
     * Fires when a field annotated with @NotBlank, @Email, @Size etc. fails its constraint.
     * Returns 400 with a map of field names -> validation messages.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String,Object> body = base(status.value(), "Validation failed", request);
        body.put("errors", ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a,b)->a)));
        return new ResponseEntity<>(body, HttpStatus.valueOf(status.value()));
    }

    /**
     * Handles IllegalArgumentException thrown anywhere in the service or domain layer.
     * Value object constructors (Email, Username etc.) throw this when input is invalid.
     * Returns 400 Bad Request — the client sent data that failed domain validation.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    /**
     * Handles IllegalStateException thrown by service logic.
     * Used for business rule conflicts, e.g. duplicate email registration, workflow already active.
     * Returns 409 Conflict — the request is valid but conflicts with current server state.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalState(IllegalStateException ex, WebRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    /**
     * Catch-all handler for any unhandled exception.
     * Returns 500 Internal Server Error with a generic message.
     * Never exposes internal exception details to the client for security reasons.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleOther(Exception ex, WebRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", req);
    }

    /// Convenience method to wrap the base error map in a ResponseEntity
    private ResponseEntity<Object> build(HttpStatus status, String message, WebRequest req) {
        return new ResponseEntity<>(base(status.value(), message, req), status);
    }

    /// Builds the standard error response body shared by all handlers
    private Map<String,Object> base(int status, String message, WebRequest req) {
        Map<String,Object> m = new HashMap<>();
        m.put("timestamp", Instant.now().toString());
        m.put("status", status);
        m.put("message", message);
        return m;
    }
}
