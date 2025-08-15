package com.crescendo.auth.dto;

import java.time.Instant;
import java.util.List;

/**
 * Returned after successful registration (and optional auto-login issuing tokens).
 */
public record RegisterResponseDTO(
	String userId,
	String email,
	String username,
	String role,
	List<String> providers,
	boolean hasLocalCredential,
	String accessToken,
	Instant accessExpiresAt,
	String refreshToken,
	Instant refreshExpiresAt,
	String message
) {}
