package com.crescendo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Initiates password reset by email.
 */
public record ForgotPasswordRequestDTO(
	@Email @NotBlank String email
) {}
