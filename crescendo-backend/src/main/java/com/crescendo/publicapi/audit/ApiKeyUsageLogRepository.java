package com.crescendo.publicapi.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ApiKeyUsageLogRepository extends JpaRepository<ApiKeyUsageLog, UUID> {
    Page<ApiKeyUsageLog> findByApiKeyIdAndUserIdOrderByCreatedAtDesc(UUID apiKeyId, UUID userId, Pageable pageable);
}
