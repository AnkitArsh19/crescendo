package com.crescendo.connections.connections_query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface Connections_queryRepository extends JpaRepository<Connections_query, UUID> {
}
