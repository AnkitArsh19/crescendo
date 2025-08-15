package com.crescendo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration request for local credential signup.
 */
public record RegisterRequestDTO(
	@Email @NotBlank String email,
	@NotBlank @Size(min = 3, max = 50) String username,
	@NotBlank @Size(min = 6) String password
) {}
