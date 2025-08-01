package com.crescendo.connections.connections_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface Connections_commandRepository extends JpaRepository<Connections_command, UUID> {
}
