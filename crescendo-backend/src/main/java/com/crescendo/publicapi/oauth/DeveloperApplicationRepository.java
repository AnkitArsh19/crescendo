package com.crescendo.publicapi.oauth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeveloperApplicationRepository extends JpaRepository<DeveloperApplication, String> {
    List<DeveloperApplication> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);
    Optional<DeveloperApplication> findByIdAndOwnerUserId(String id, UUID ownerUserId);
    Optional<DeveloperApplication> findByClientIdAndActiveTrue(String clientId);
    Optional<DeveloperApplication> findByIdAndActiveTrue(String id);
}
