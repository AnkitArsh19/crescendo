package com.crescendo.connections.connections_command;

import com.crescendo.app.App;
import com.crescendo.app.AppRepository;
import com.crescendo.connections.ConnectionsDto;
import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import com.crescendo.connections.connections_query.Connections_query;
import com.crescendo.connections.connections_query.Connections_queryRepository;
import com.crescendo.connections.domain_event.ConnectionCreatedEvent;
import com.crescendo.connections.domain_event.ConnectionDeletedEvent;
import com.crescendo.connections.domain_event.ConnectionUpdatedEvent;
import com.crescendo.enums.ConnectionStatus;
import com.crescendo.enums.AuthType;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.shared.domain.valueobject.AppKey;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Write-side service for connection management.
 *
 * Every mutation:
 *   1. Validates ownership
 *   2. Writes to the command database
 *   3. Synchronously projects to the query database
 *   4. Publishes a domain event
 */
@Service
@Transactional
public class Connections_commandService {

    private final Connections_commandRepository connectionRepo;
    private final Connections_queryRepository connectionQueryRepo;
    private final User_commandRepository userRepo;
    private final AppRepository appRepository;
    private final ConnectionCredentialsCryptoService cryptoService;
    private final DomainEventPublisher eventPublisher;

    public Connections_commandService(Connections_commandRepository connectionRepo,
                                     Connections_queryRepository connectionQueryRepo,
                                     User_commandRepository userRepo,
                                     AppRepository appRepository,
                                     ConnectionCredentialsCryptoService cryptoService,
                                     DomainEventPublisher eventPublisher) {
        this.connectionRepo = connectionRepo;
        this.connectionQueryRepo = connectionQueryRepo;
        this.userRepo = userRepo;
        this.appRepository = appRepository;
        this.cryptoService = cryptoService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new connection for a user.
     */
    public ConnectionsDto.ConnectionResponse createConnection(UUID userId,
                                                               ConnectionsDto.CreateConnectionRequest req) {
        User_command user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UUID connectionId = UUID.randomUUID();
        AppKey appKey = AppKey.of(req.appKey());
        App app = appRepository.findById(appKey)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown appKey: " + req.appKey()));
        validateCredentialsForAuthType(app.getAuthType(), req.credentials());
        var encryptedCredentials = cryptoService.seal(req.credentials());

        Connections_command connection = new Connections_command(
            connectionId, user, appKey, req.name(), encryptedCredentials, ConnectionStatus.ACTIVE);
        connectionRepo.save(connection);

        // Sync to query database (no credentials on read side)
        projectToQuery(connection, userId);

        eventPublisher.publish(new ConnectionCreatedEvent(connectionId, userId, req.appKey()));

        return toResponse(connection, userId);
    }

    /**
     * Updates a connection's name and/or credentials.
     */
    public void updateConnection(UUID userId, UUID connectionId,
                                 ConnectionsDto.UpdateConnectionRequest req) {
        Connections_command connection = findOwnedConnection(userId, connectionId);

        if (req.name() == null && req.credentials() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field must be provided");
        }

        if (req.name() != null) {
            connection.setName(req.name());
        }
        if (req.credentials() != null) {
            App app = appRepository.findById(AppKey.of(connection.getAppKey()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown appKey: " + connection.getAppKey()));
            validateCredentialsForAuthType(app.getAuthType(), req.credentials());
            connection.setCredentials(cryptoService.seal(req.credentials()));
            connection.setStatus(ConnectionStatus.ACTIVE);
        }

        // Sync name/status to query side
        connectionQueryRepo.findById(connectionId).ifPresent(q -> {
            if (req.name() != null) q.setName(req.name());
            if (req.credentials() != null) q.setStatus(ConnectionStatus.ACTIVE);
        });

        eventPublisher.publish(new ConnectionUpdatedEvent(connectionId));
    }

    /**
     * Deletes a connection permanently.
     */
    public void deleteConnection(UUID userId, UUID connectionId) {
        Connections_command connection = findOwnedConnection(userId, connectionId);

        connectionRepo.delete(connection);
        connectionQueryRepo.deleteById(connectionId);

        eventPublisher.publish(new ConnectionDeletedEvent(connectionId));
    }

    // QUERY-SIDE PROJECTION

    private void projectToQuery(Connections_command connection, UUID userId) {
        Connections_query q = new Connections_query(
                connection.getId(),
                userId,
                connection.getAppKey(),
                connection.getName(),
                connection.getStatus()
        );
        connectionQueryRepo.save(q);
    }

    // OWNERSHIP VERIFICATION

    private Connections_command findOwnedConnection(UUID userId, UUID connectionId) {
        return connectionRepo.findByIdAndUser_Id(connectionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found"));
    }

    private void validateCredentialsForAuthType(AuthType authType, java.util.Map<String, Object> credentials) {
        if (authType == AuthType.NONE) {
            if (credentials != null && !credentials.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This app does not require credentials. Leave credentials empty.");
            }
            return;
        }

        if (credentials == null || credentials.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Credentials are required for auth type " + authType.name());
        }

        boolean hasUsableSecret = credentials.entrySet().stream()
                .anyMatch(e -> isSecretKey(e.getKey()) && !isBlankValue(e.getValue()));

        if (!hasUsableSecret) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Credentials must include at least one non-empty secret field (e.g., apiKey, accessToken, refreshToken, clientSecret)");
        }
    }

    private boolean isSecretKey(String key) {
        if (key == null) return false;
        String k = key.toLowerCase();
        return k.contains("token")
                || k.contains("secret")
                || k.contains("apikey")
                || k.equals("api_key")
                || k.equals("apiKey");
    }

    private boolean isBlankValue(Object value) {
        if (value == null) return true;
        if (value instanceof String s) return s.isBlank();
        return false;
    }

    // DTO MAPPER

    private ConnectionsDto.ConnectionResponse toResponse(Connections_command c, UUID userId) {
        return new ConnectionsDto.ConnectionResponse(
                c.getId(),
                c.getAppKey(),
                c.getName(),
                c.getStatus().name(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
