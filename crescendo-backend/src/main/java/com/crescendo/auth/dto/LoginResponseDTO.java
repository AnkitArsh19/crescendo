package com.crescendo.auth.dto;

import java.time.Instant;
import java.util.List;

/**
 * Returned after successful login (password or OAuth) or initial registration auto-login.
 * Includes expiry timestamps so clients can schedule refresh.
 */
public record LoginResponseDTO(
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
	String profilePictureUrl // nullable / optional
) {}
