package com.crescendo.auth.dto;

import java.time.Instant;

/**
 * Response for a refresh operation. If rotation is enabled, a new refresh token + expiry is returned.
 * If rotation is disabled, refresh token fields may repeat prior values.
 */
public record AccessTokenResponseDTO(
	String accessToken,
	Instant accessExpiresAt,
	String refreshToken,
	Instant refreshExpiresAt
) {}
