package com.crescendo.publicapi;

import com.crescendo.connections.ConnectionsDto;
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
 * Public developer API for connection management.
 * Accessible via API key (Bearer re_...) or JWT authentication.
 *
 * Connections store credentials for third-party apps (OAuth tokens, API keys, etc.)
 * used by workflow steps. Credentials are write-only — never returned in responses.
 *
 *   POST   /api/v1/connections           — create a new connection
 *   GET    /api/v1/connections           — list all connections (metadata only)
 *   GET    /api/v1/connections/{id}      — get connection detail (metadata only)
 *   PATCH  /api/v1/connections/{id}      — update name / credentials
 *   DELETE /api/v1/connections/{id}      — delete connection
 */
@RestController
@RequestMapping("/api/v1/connections")
public class PublicConnectionController {

    private final Connections_commandService commandService;
    private final Connections_queryService queryService;

    public PublicConnectionController(Connections_commandService commandService,
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
