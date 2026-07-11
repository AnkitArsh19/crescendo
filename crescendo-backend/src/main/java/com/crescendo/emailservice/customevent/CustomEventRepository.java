package com.crescendo.emailservice.customevent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomEventRepository extends JpaRepository<CustomEvent, UUID> {
    List<CustomEvent> findByUserId(UUID userId);
    Optional<CustomEvent> findByUserIdAndName(UUID userId, String name);
    void deleteByUserIdAndName(UUID userId, String name);
}
