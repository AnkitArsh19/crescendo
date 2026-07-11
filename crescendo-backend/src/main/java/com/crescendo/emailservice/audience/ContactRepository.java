package com.crescendo.emailservice.audience;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {

    List<Contact> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Contact> findByUserIdAndSubscribedTrue(UUID userId);

    Optional<Contact> findByIdAndUserId(UUID id, UUID userId);

    Optional<Contact> findByUserIdAndEmail(UUID userId, String email);

    boolean existsByUserIdAndEmail(UUID userId, String email);

    long countByUserId(UUID userId);

    long countByUserIdAndSubscribedTrue(UUID userId);
}
