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
import org.springframework.stereotype.Service;

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
    private final Map<SnapshotKey, Snapshot> snapshotCache = new ConcurrentHashMap<>();

    public AiContextService(Connections_queryService connectionsQueryService,
                            ResourceFetchService resourceFetchService,
                            ResourceProviderRegistry resourceProviderRegistry) {
        this.connectionsQueryService = connectionsQueryService;
        this.resourceFetchService = resourceFetchService;
        this.resourceProviderRegistry = resourceProviderRegistry;
    }

    public Map<String, Object> buildContext(UUID userId, Map<String, Object> callerContext) {
        Map<String, Object> context = callerContext != null ? new LinkedHashMap<>(callerContext) : new LinkedHashMap<>();
        List<ConnectionsDto.ConnectionResponse> connections = connectionsQueryService.listConnections(userId);

        context.put("connections", connections.stream().map(connection -> Map.<String, Object>of(
                "connectionId", connection.id().toString(),
                "appKey", connection.appKey(),
                "label", Objects.requireNonNullElse(connection.name(), connection.appKey())
        )).toList());

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
                    log.warn("[workflow-context] Resource snapshot skipped for {}:{} connection={}: {}",
                            connection.appKey(), descriptor.resourceType(), connection.id(), exception.getMessage());
                }
            }
        }
        context.put("resources", resources);
        return context;
    }

    private List<ResourceOption> snapshot(UUID userId, UUID connectionId, String appKey,
                                          ResourceContextDescriptor descriptor) {
        SnapshotKey key = new SnapshotKey(userId, connectionId, descriptor.resourceType());
        Snapshot cached = snapshotCache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.items();
        List<ResourceOption> options = resourceFetchService.fetchResources(appKey, descriptor.resourceType(),
                connectionId, userId, Map.of()).stream().limit(descriptor.maxItems()).toList();
        snapshotCache.put(key, new Snapshot(options, Instant.now().plus(descriptor.cacheTtl())));
        return options;
    }

    private record SnapshotKey(UUID userId, UUID connectionId, String resourceType) { }
    private record Snapshot(List<ResourceOption> items, Instant expiresAt) { }
}
