package com.crescendo.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Client submits refresh token to obtain a new access token (and possibly rotated refresh token).
 */
public record RefreshTokenRequestDTO(
	@NotBlank String refreshToken
) {}
