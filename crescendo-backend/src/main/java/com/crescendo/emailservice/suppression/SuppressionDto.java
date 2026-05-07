package com.crescendo.emailservice.suppression;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class SuppressionDto {

    private SuppressionDto() {}

    public record AddSuppressionRequest(
            @NotBlank @Email String email
    ) {}

    public record SuppressionResponse(
            UUID id,
            String email,
            String reason,
            Instant createdAt
    ) {}
}
