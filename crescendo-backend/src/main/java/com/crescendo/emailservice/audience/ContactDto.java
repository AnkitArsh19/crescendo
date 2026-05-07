package com.crescendo.emailservice.audience;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class ContactDto {

    private ContactDto() {}

    public record CreateContactRequest(
            @NotBlank @Email String email,
            String firstName,
            String lastName
    ) {}

    public record UpdateContactRequest(
            String firstName,
            String lastName,
            Boolean subscribed
    ) {}

    public record ContactResponse(
            UUID id,
            String email,
            String firstName,
            String lastName,
            boolean subscribed,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
