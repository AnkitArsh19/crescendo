package com.crescendo.publicapi;

import com.crescendo.connections.ConnectionsDto;
import com.crescendo.connections.connections_command.Connections_commandService;
import com.crescendo.connections.connections_query.Connections_queryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;
import static com.crescendo.publicapi.PublicApiScopes.*;

import java.util.List;
import java.util.UUID;

/**
 * Public developer API for connection management.
 * Accessible via API key (Bearer re_...) or JWT authentication.
 *
 * Connections store credentials for third-party apps (OAuth tokens, API keys, etc.)
 * used by workflow steps. Credentials are write-only — never returned in responses.
 */
@RestController
@RequestMapping("/api/v1/connections")
@Tag(name = "Connections", description = "Public API for managing third-party app credentials")
public class PublicConnectionController {

    private final Connections_commandService commandService;
    private final Connections_queryService queryService;

    public PublicConnectionController(Connections_commandService commandService,
                                      Connections_queryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @Operation(summary = "Create connection", description = "Creates a new connection for a third-party app. Requires connection:write scope.")
    public ResponseEntity<ConnectionsDto.ConnectionResponse> createConnection(
            @Valid @RequestBody ConnectionsDto.CreateConnectionRequest req,
            Authentication auth) {
        require(auth, CONNECTION_WRITE);
        var resp = commandService.createConnection(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    @Operation(summary = "List connections", description = "Lists all connections. Requires connection:read scope.")
    public ResponseEntity<List<ConnectionsDto.ConnectionResponse>> listConnections(Authentication auth) {
        require(auth, CONNECTION_READ);
        return ResponseEntity.ok(queryService.listConnections(userId(auth)));
    }

    @GetMapping("/{connectionId}")
    @Operation(summary = "Get connection details", description = "Gets metadata for a specific connection. Credentials are never returned. Requires connection:read scope.")
    public ResponseEntity<ConnectionsDto.ConnectionResponse> getConnection(
            @PathVariable UUID connectionId,
            Authentication auth) {
        require(auth, CONNECTION_READ);
        return ResponseEntity.ok(queryService.getConnection(userId(auth), connectionId));
    }

    @PatchMapping("/{connectionId}")
    @Operation(summary = "Update connection", description = "Updates a connection's name or credentials. Requires connection:write scope.")
    public ResponseEntity<Void> updateConnection(
            @PathVariable UUID connectionId,
            @Valid @RequestBody ConnectionsDto.UpdateConnectionRequest req,
            Authentication auth) {
        require(auth, CONNECTION_WRITE);
        commandService.updateConnection(userId(auth), connectionId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{connectionId}")
    @Operation(summary = "Delete connection", description = "Deletes a connection. Requires connection:write scope.")
    public ResponseEntity<Void> deleteConnection(
            @PathVariable UUID connectionId,
            Authentication auth) {
        require(auth, CONNECTION_WRITE);
        commandService.deleteConnection(userId(auth), connectionId);
        return ResponseEntity.noContent().build();
    }
}
