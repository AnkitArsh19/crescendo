package com.crescendo.security;

import java.time.Instant;

/**
 * Returned after issuing or refreshing tokens.
 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        Instant accessExpiresAt,
        Instant refreshExpiresAt
) {}
