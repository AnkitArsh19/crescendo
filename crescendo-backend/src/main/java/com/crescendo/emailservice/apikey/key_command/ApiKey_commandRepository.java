package com.crescendo.emailservice.apikey.key_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKey_commandRepository extends JpaRepository<ApiKey_command, UUID> {

    Optional<ApiKey_command> findByPrefix(String prefix);

    boolean existsByPrefix(String prefix);

    List<ApiKey_command> findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<ApiKey_command> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndRevokedAtIsNull(UUID userId);
}
