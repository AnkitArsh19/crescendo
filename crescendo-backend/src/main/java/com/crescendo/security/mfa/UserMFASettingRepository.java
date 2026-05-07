package com.crescendo.security.mfa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMFASettingRepository extends JpaRepository<UserMFASetting, UUID> {

    // Derived query method (no @Query needed)
    Optional<UserMFASetting> findByUser_Id(UUID userId);
}
