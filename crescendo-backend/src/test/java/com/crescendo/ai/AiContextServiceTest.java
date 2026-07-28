package com.crescendo.ai;

import com.crescendo.connections.ConnectionsDto;
import com.crescendo.connections.connections_query.Connections_queryService;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceFetchService;
import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import com.crescendo.execution.resource.ResourceProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiContextServiceTest {

    @Mock private Connections_queryService connectionsQueryService;
    @Mock private ResourceFetchService resourceFetchService;
    @Mock private ResourceProviderRegistry resourceProviderRegistry;
    @Mock private ResourceProvider slackProvider;
    @Mock private ResourceProvider notionProvider;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final UUID userId = UUID.randomUUID();
    private final UUID slackConnectionId = UUID.randomUUID();
    private final UUID notionConnectionId = UUID.randomUUID();
    private AiContextService service;

    @BeforeEach
    void setUp() {
        service = new AiContextService(connectionsQueryService, resourceFetchService, resourceProviderRegistry, clock);
        when(connectionsQueryService.listConnections(userId)).thenReturn(List.of(
                connection(slackConnectionId, "slack", "Engineering Slack"),
                connection(notionConnectionId, "notion", "Team wiki")
        ));
    }

    @Test
    void aggregatesConnectorDeclaredResources_andUsesPerConnectionTtlCache() {
        ResourceContextDescriptor channels = descriptor("channels", 2);
        ResourceContextDescriptor databases = descriptor("databases", 2);
        when(resourceProviderRegistry.find("slack")).thenReturn(Optional.of(slackProvider));
        when(resourceProviderRegistry.find("notion")).thenReturn(Optional.of(notionProvider));
        when(slackProvider.contextResourceDescriptors()).thenReturn(Set.of(channels));
        when(notionProvider.contextResourceDescriptors()).thenReturn(Set.of(databases));
        when(resourceFetchService.fetchResources(eq("slack"), eq("channels"), eq(slackConnectionId), eq(userId), anyMap()))
                .thenReturn(List.of(new ResourceOption("C1", "general"), new ResourceOption("C2", null, "Private"), new ResourceOption("C3", "ignored")));
        when(resourceFetchService.fetchResources(eq("notion"), eq("databases"), eq(notionConnectionId), eq(userId), anyMap()))
                .thenReturn(List.of(new ResourceOption("DB1", "Projects", "Planning")));

        Map<String, Object> first = service.buildContext(userId, Map.of("requestId", "draft-1"));
        Map<String, Object> second = service.buildContext(userId, Map.of("requestId", "draft-2"));

        assertEquals("draft-1", first.get("requestId"));
        assertEquals(List.of(
                Map.of("connectionId", slackConnectionId.toString(), "appKey", "slack", "label", "Engineering Slack"),
                Map.of("connectionId", notionConnectionId.toString(), "appKey", "notion", "label", "Team wiki")
        ), first.get("connections"));
        assertEquals(List.of(
                Map.of("connectionId", slackConnectionId.toString(), "appKey", "slack", "resourceType", "channels", "items", List.of(
                        Map.of("id", "C1", "label", "general", "description", ""),
                        Map.of("id", "C2", "label", "C2", "description", "Private")
                )),
                Map.of("connectionId", notionConnectionId.toString(), "appKey", "notion", "resourceType", "databases", "items", List.of(
                        Map.of("id", "DB1", "label", "Projects", "description", "Planning")
                ))
        ), first.get("resources"));
        assertEquals("draft-2", second.get("requestId"));
        verify(resourceFetchService).fetchResources(eq("slack"), eq("channels"), eq(slackConnectionId), eq(userId), anyMap());
        verify(resourceFetchService).fetchResources(eq("notion"), eq("databases"), eq(notionConnectionId), eq(userId), anyMap());

        clock.advance(Duration.ofMinutes(6));
        service.buildContext(userId, Map.of());

        verify(resourceFetchService, org.mockito.Mockito.times(2))
                .fetchResources(eq("slack"), eq("channels"), eq(slackConnectionId), eq(userId), anyMap());
        verify(resourceFetchService, org.mockito.Mockito.times(2))
                .fetchResources(eq("notion"), eq("databases"), eq(notionConnectionId), eq(userId), anyMap());
    }

    @Test
    void isolatesOneConnectorFailure_andKeepsOtherContextAvailable() {
        when(resourceProviderRegistry.find("slack")).thenReturn(Optional.of(slackProvider));
        when(resourceProviderRegistry.find("notion")).thenReturn(Optional.of(notionProvider));
        when(slackProvider.contextResourceDescriptors()).thenReturn(Set.of(descriptor("channels", 10)));
        when(notionProvider.contextResourceDescriptors()).thenReturn(Set.of(descriptor("databases", 10)));
        doThrow(new IllegalStateException("Slack timed out"))
                .when(resourceFetchService).fetchResources(eq("slack"), eq("channels"), eq(slackConnectionId), eq(userId), anyMap());
        when(resourceFetchService.fetchResources(eq("notion"), eq("databases"), eq(notionConnectionId), eq(userId), anyMap()))
                .thenReturn(List.of(new ResourceOption("DB1", "Projects", "Planning")));

        Map<String, Object> context = service.buildContext(userId, Map.of());

        assertEquals(List.of(Map.of(
                "connectionId", notionConnectionId.toString(),
                "appKey", "notion",
                "resourceType", "databases",
                "items", List.of(Map.of("id", "DB1", "label", "Projects", "description", "Planning"))
        )), context.get("resources"));
    }

    private static ConnectionsDto.ConnectionResponse connection(UUID id, String appKey, String name) {
        return new ConnectionsDto.ConnectionResponse(id, appKey, name, "ACTIVE", Instant.EPOCH, Instant.EPOCH, null);
    }

    private static ResourceContextDescriptor descriptor(String type, int maxItems) {
        return new ResourceContextDescriptor(type, maxItems, Duration.ofMinutes(5));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
