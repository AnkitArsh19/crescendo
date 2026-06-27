package com.crescendo.security;

import com.crescendo.publicapi.oauth.OAuthConsumedRefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OAuthConsumedRefreshTokenCleaner {
    private final OAuthConsumedRefreshTokenRepository consumedTokens;

    public OAuthConsumedRefreshTokenCleaner(OAuthConsumedRefreshTokenRepository consumedTokens) {
        this.consumedTokens = consumedTokens;
    }

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void removeExpiredHistory() {
        consumedTokens.deleteByExpiresAtBefore(Instant.now());
    }
}
