package com.crescendo.publicapi.oauth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OAuthAccessTokenUsageLogRepository
        extends JpaRepository<OAuthAccessTokenUsageLog, UUID> {
    Page<OAuthAccessTokenUsageLog> findByApplicationIdOrderByCreatedAtDesc(
            String applicationId,
            Pageable pageable
    );
}
