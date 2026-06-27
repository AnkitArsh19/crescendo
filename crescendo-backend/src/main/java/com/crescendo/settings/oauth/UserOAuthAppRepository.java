package com.crescendo.settings.oauth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOAuthAppRepository extends JpaRepository<UserOAuthApp, UUID> {

    List<UserOAuthApp> findByUserId(UUID userId);

    Optional<UserOAuthApp> findByUserIdAndProviderKey(UUID userId, String providerKey);

    void deleteByUserIdAndProviderKey(UUID userId, String providerKey);
}
