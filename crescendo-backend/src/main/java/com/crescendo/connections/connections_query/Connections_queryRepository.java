package com.crescendo.connections.connections_query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface Connections_queryRepository extends JpaRepository<Connections_query, UUID> {

    List<Connections_query> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Connections_query> findByIdAndUserId(UUID id, UUID userId);
}
