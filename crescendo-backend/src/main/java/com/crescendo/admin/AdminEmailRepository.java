package com.crescendo.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminEmailRepository extends JpaRepository<AdminEmail, UUID> {
    Optional<AdminEmail> findByEmail(String email);
    boolean existsByEmail(String email);
    void deleteByEmail(String email);
}
