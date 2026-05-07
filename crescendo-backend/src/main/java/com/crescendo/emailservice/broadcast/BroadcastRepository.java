package com.crescendo.emailservice.broadcast;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BroadcastRepository extends JpaRepository<Broadcast, UUID> {

    List<Broadcast> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Broadcast> findByIdAndUserId(UUID id, UUID userId);
}
