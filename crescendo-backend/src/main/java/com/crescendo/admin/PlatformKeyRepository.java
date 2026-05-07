package com.crescendo.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformKeyRepository extends JpaRepository<PlatformKey, UUID> {
    Optional<PlatformKey> findByAppKey(String appKey);
    Optional<PlatformKey> findByAppKeyAndEnabledTrue(String appKey);
    boolean existsByAppKey(String appKey);
    List<PlatformKey> findAllByEnabledTrue();
}
