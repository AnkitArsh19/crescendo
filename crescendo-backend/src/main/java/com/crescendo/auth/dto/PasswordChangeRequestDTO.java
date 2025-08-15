package com.crescendo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Authenticated user changing their password (old -> new).
 */
public record PasswordChangeRequestDTO(
	@NotBlank String oldPassword,
	@NotBlank @Size(min = 6) String newPassword
) {}
