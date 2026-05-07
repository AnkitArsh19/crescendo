package com.crescendo.emailservice.apikey.key_query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKey_queryRepository extends JpaRepository<ApiKey_query, UUID> {

    List<ApiKey_query> findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<ApiKey_query> findByIdAndUserId(UUID id, UUID userId);
}
