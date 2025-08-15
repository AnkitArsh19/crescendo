package com.crescendo.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login using either email or username. Client sends whatever they have in 'identifier'.
 */
public record LoginRequestDTO(
	@NotBlank String identifier,
	@NotBlank String password
) {}
