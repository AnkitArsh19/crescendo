package com.crescendo.emailservice.apikey.key_query;

import com.crescendo.emailservice.apikey.ApiKeyDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Read-side service for API keys.
 * Returns metadata only — never exposes hashed keys.
 */
@Service
@Transactional(readOnly = true)
public class ApiKey_queryService {

    private final ApiKey_queryRepository queryRepo;

    public ApiKey_queryService(ApiKey_queryRepository queryRepo) {
        this.queryRepo = queryRepo;
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

    private ApiKeyDto.ApiKeyResponse toResponse(ApiKey_query key) {
        return new ApiKeyDto.ApiKeyResponse(
                key.getId(),
                key.getName(),
                key.getPrefix(),
                key.getCreatedAt(),
                key.getLastUsedAt()
        );
    }
}
