package com.crescendo.connections;

import com.crescendo.connections.connections_command.Connections_commandService;
import com.crescendo.connections.connections_query.Connections_queryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated connection endpoints under /connections.
 *
 * Connections store user credentials for third-party apps.
 * Credential data is never returned in responses — only metadata.
 *
 *   POST   /connections           — create a new connection
 *   GET    /connections           — list all connections for user
 *   GET    /connections/{id}      — get connection detail
 *   PATCH  /connections/{id}      — update name / credentials
 *   DELETE /connections/{id}      — delete connection
 */
@RestController
@RequestMapping("/connections")
public class ConnectionsController {

    private final Connections_commandService commandService;
    private final Connections_queryService queryService;

    public ConnectionsController(Connections_commandService commandService,
                                 Connections_queryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<ConnectionsDto.ConnectionResponse> createConnection(
            @Valid @RequestBody ConnectionsDto.CreateConnectionRequest req,
            Authentication auth) {
        var resp = commandService.createConnection(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<ConnectionsDto.ConnectionResponse>> listConnections(Authentication auth) {
        return ResponseEntity.ok(queryService.listConnections(userId(auth)));
    }

    @GetMapping("/{connectionId}")
    public ResponseEntity<ConnectionsDto.ConnectionResponse> getConnection(
            @PathVariable UUID connectionId,
            Authentication auth) {
        return ResponseEntity.ok(queryService.getConnection(userId(auth), connectionId));
    }

    @PatchMapping("/{connectionId}")
    public ResponseEntity<Void> updateConnection(
            @PathVariable UUID connectionId,
            @Valid @RequestBody ConnectionsDto.UpdateConnectionRequest req,
            Authentication auth) {
        commandService.updateConnection(userId(auth), connectionId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> deleteConnection(
            @PathVariable UUID connectionId,
            Authentication auth) {
        commandService.deleteConnection(userId(auth), connectionId);
        return ResponseEntity.noContent().build();
    }

}
