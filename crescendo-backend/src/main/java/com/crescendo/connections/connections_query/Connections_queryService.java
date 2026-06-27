package com.crescendo.connections.connections_query;

import com.crescendo.connections.ConnectionsDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Read-side service for connection queries.
 * Returns credential-free projections from the query database.
 */
@Service
public class Connections_queryService {

    private final Connections_queryRepository queryRepo;

    public Connections_queryService(Connections_queryRepository queryRepo) {
        this.queryRepo = queryRepo;
    }

    public List<ConnectionsDto.ConnectionResponse> listConnections(UUID userId) {
        return queryRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ConnectionsDto.ConnectionResponse getConnection(UUID userId, UUID connectionId) {
        Connections_query connection = queryRepo.findByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found"));
        return toResponse(connection);
    }

    private ConnectionsDto.ConnectionResponse toResponse(Connections_query c) {
        return new ConnectionsDto.ConnectionResponse(
                c.getId(),
                c.getAppKey(),
                c.getName(),
                c.getStatus().name(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getGrantedScopes()
        );
    }
}
