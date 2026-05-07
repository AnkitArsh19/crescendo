package com.crescendo.connections.connections_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface Connections_commandRepository extends JpaRepository<Connections_command, UUID> {

    List<Connections_command> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<Connections_command> findByIdAndUser_Id(UUID id, UUID userId);
}
