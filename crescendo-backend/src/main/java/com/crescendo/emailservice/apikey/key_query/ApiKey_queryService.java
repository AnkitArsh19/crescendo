package com.crescendo.emailservice.apikey.key_query;

import com.crescendo.emailservice.apikey.ApiKeyDto;
import com.crescendo.publicapi.PublicApiScopes;
import com.crescendo.publicapi.audit.ApiKeyUsageLog;
import com.crescendo.publicapi.audit.ApiKeyUsageLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

/**
 * Read-side service for API keys.
 * Returns metadata only — never exposes hashed keys.
 */
@Service
@Transactional(readOnly = true)
public class ApiKey_queryService {

    private final ApiKey_queryRepository queryRepo;
    private final ApiKeyUsageLogRepository usageLogRepo;

    public ApiKey_queryService(ApiKey_queryRepository queryRepo,
                               ApiKeyUsageLogRepository usageLogRepo) {
        this.queryRepo = queryRepo;
        this.usageLogRepo = usageLogRepo;
    }

    public List<ApiKeyDto.ApiKeyResponse> listApiKeys(UUID userId) {
        return queryRepo.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ApiKeyDto.ApiKeyResponse getApiKey(UUID userId, UUID apiKeyId) {
        ApiKey_query key = queryRepo.findByIdAndUserId(apiKeyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
        return toResponse(key);
    }

    public Page<ApiKeyDto.ApiKeyUsageResponse> listUsage(UUID userId, UUID apiKeyId, Pageable pageable) {
        queryRepo.findByIdAndUserId(apiKeyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
        return usageLogRepo.findByApiKeyIdAndUserIdOrderByCreatedAtDesc(apiKeyId, userId, pageable)
                .map(this::toUsageResponse);
    }

    private ApiKeyDto.ApiKeyResponse toResponse(ApiKey_query key) {
        return new ApiKeyDto.ApiKeyResponse(
                key.getId(),
                key.getName(),
                key.getPrefix(),
                PublicApiScopes.parse(key.getScopes()),
                key.getRateLimitPerMinute(),
                key.getCreatedAt(),
                key.getLastUsedAt(),
                key.getExpiresAt(),
                key.getRotationGraceEndsAt(),
                status(key, Instant.now())
        );
    }

    private String status(ApiKey_query key, Instant now) {
        if (key.getRevokedAt() != null) {
            return "REVOKED";
        }
        if (key.getExpiresAt() != null && !key.getExpiresAt().isAfter(now)) {
            return "EXPIRED";
        }
        if (key.getRotationGraceEndsAt() != null && !key.getRotationGraceEndsAt().isAfter(now)) {
            return "ROTATED";
        }
        return key.getReplacedByKeyId() == null ? "ACTIVE" : "ROTATING";
    }

    private ApiKeyDto.ApiKeyUsageResponse toUsageResponse(ApiKeyUsageLog log) {
        return new ApiKeyDto.ApiKeyUsageResponse(
                log.getId(),
                log.getApiKeyId(),
                log.getMethod(),
                log.getPath(),
                log.getStatus(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt()
        );
    }
}
