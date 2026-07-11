package com.crescendo.publicapi.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shared error response shape used across all public API endpoints.
 *
 * Every 4xx and 5xx response from the Crescendo API returns this structure.
 * Generated SDK clients will use this as the typed exception payload.
 *
 * Example:
 * <pre>
 * {
 *   "type": "invalid_request_error",
 *   "message": "from address is required",
 *   "status": 400
 * }
 * </pre>
 */
@Schema(name = "ErrorResponse", description = "Standard error envelope returned by all Crescendo API endpoints")
public record ErrorResponse(

        @Schema(
            description = "Machine-readable error type",
            example = "invalid_request_error",
            allowableValues = {
                "invalid_request_error",
                "unauthorized_error",
                "forbidden_error",
                "not_found_error",
                "conflict_error",
                "rate_limit_error",
                "api_error"
            }
        )
        String type,

        @Schema(description = "Human-readable error message", example = "from address is required")
        String message,

        @Schema(description = "HTTP status code", example = "400")
        int status
) {}
