package com.crescendo.ai;

import com.crescendo.connections.ConnectionsDto;
import com.crescendo.connections.connections_query.Connections_queryService;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceFetchService;
import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import com.crescendo.execution.resource.ResourceProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Builds bounded, connector-declared context for a workflow draft request. */
@Service
public class AiContextService {
    private static final Logger log = LoggerFactory.getLogger(AiContextService.class);

    private final Connections_queryService connectionsQueryService;
    private final ResourceFetchService resourceFetchService;
    private final ResourceProviderRegistry resourceProviderRegistry;
    private final Clock clock;
    private final Map<SnapshotKey, Snapshot> snapshotCache = new ConcurrentHashMap<>();

    @Autowired
    public AiContextService(Connections_queryService connectionsQueryService,
                            ResourceFetchService resourceFetchService,
                            ResourceProviderRegistry resourceProviderRegistry) {
        this(connectionsQueryService, resourceFetchService, resourceProviderRegistry, Clock.systemUTC());
    }

    /** Package-visible clock seam keeps snapshot expiry deterministic in unit tests. */
    AiContextService(Connections_queryService connectionsQueryService,
                     ResourceFetchService resourceFetchService,
                     ResourceProviderRegistry resourceProviderRegistry,
                     Clock clock) {
        this.connectionsQueryService = connectionsQueryService;
        this.resourceFetchService = resourceFetchService;
        this.resourceProviderRegistry = resourceProviderRegistry;
        this.clock = clock;
    }

    public Map<String, Object> buildContext(UUID userId, Map<String, Object> callerContext) {
        Map<String, Object> context = callerContext != null ? new LinkedHashMap<>(callerContext) : new LinkedHashMap<>();
        List<ConnectionsDto.ConnectionResponse> connections = connectionsQueryService.listConnections(userId);

        Map<String, Map<String, Object>> connectionMap = new LinkedHashMap<>();
        for (ConnectionsDto.ConnectionResponse connection : connections) {
            Map<String, Object> connData = new LinkedHashMap<>();
            connData.put("connectionId", connection.id().toString());
            connData.put("appKey", connection.appKey());
            connData.put("label", Objects.requireNonNullElse(connection.name(), connection.appKey()));
            connData.put("status", connection.status() != null ? connection.status() : "ACTIVE");
            connectionMap.put(connection.id().toString(), connData);
        }

        List<Map<String, Object>> resources = new ArrayList<>();
        for (ConnectionsDto.ConnectionResponse connection : connections) {
            Optional<ResourceProvider> provider = resourceProviderRegistry.find(connection.appKey());
            if (provider.isEmpty()) continue;
            for (ResourceContextDescriptor descriptor : provider.get().contextResourceDescriptors()) {
                try {
                    List<ResourceOption> options = snapshot(userId, connection.id(), connection.appKey(), descriptor);
                    if (options.isEmpty()) continue;
                    resources.add(Map.of(
                            "connectionId", connection.id().toString(),
                            "appKey", connection.appKey(),
                            "resourceType", descriptor.resourceType(),
                            "items", options.stream().map(option -> Map.of(
                                    "id", option.id(),
                                    "label", Objects.requireNonNullElse(option.label(), option.id()),
                                    "description", Objects.requireNonNullElse(option.description(), "")
                            )).toList()
                    ));
                } catch (Exception exception) {
                    Map<String, Object> connData = connectionMap.get(connection.id().toString());
                    if (connData != null) {
                        connData.put("status", "REAUTH");
                    }
                    log.warn("[workflow-context] Resource snapshot skipped for {}:{} connection={}: {}",
                            connection.appKey(), descriptor.resourceType(), connection.id(), exception.getMessage());
                }
            }
        }
        context.put("connections", new ArrayList<>(connectionMap.values()));
        context.put("resources", resources);
        return context;
    }

    private List<ResourceOption> snapshot(UUID userId, UUID connectionId, String appKey,
                                          ResourceContextDescriptor descriptor) {
        SnapshotKey key = new SnapshotKey(userId, connectionId, descriptor.resourceType());
        Snapshot cached = snapshotCache.get(key);
        Instant now = Instant.now(clock);
        if (cached != null && cached.expiresAt().isAfter(now)) return cached.items();
        List<ResourceOption> options = resourceFetchService.fetchResources(appKey, descriptor.resourceType(),
                connectionId, userId, Map.of()).stream().limit(descriptor.maxItems()).toList();
        snapshotCache.put(key, new Snapshot(options, now.plus(descriptor.cacheTtl())));
        return options;
    }

    private record SnapshotKey(UUID userId, UUID connectionId, String resourceType) { }
    private record Snapshot(List<ResourceOption> items, Instant expiresAt) { }
}
