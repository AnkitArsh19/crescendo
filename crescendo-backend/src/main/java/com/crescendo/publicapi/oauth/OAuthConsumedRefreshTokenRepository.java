package com.crescendo.publicapi.oauth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface OAuthConsumedRefreshTokenRepository
        extends JpaRepository<OAuthConsumedRefreshToken, String> {
    void deleteByAuthorizationId(String authorizationId);
    long deleteByExpiresAtBefore(Instant instant);
}
