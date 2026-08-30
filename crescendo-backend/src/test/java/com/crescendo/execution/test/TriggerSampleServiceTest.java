package com.crescendo.execution.test;

import com.crescendo.execution.resource.ResourceFetchService;
import com.crescendo.execution.resource.ResourceOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TriggerSampleServiceTest {

    private final ResourceFetchService resourceFetchService = mock(ResourceFetchService.class);
    private final TriggerSampleService service = new TriggerSampleService(resourceFetchService);

    @Test
    void scheduleTriggerReturnsScheduledTimeAndInterval() {
        Map<String, Object> sample = service.getTriggerSample("schedule", "cron", null,
                Map.of("timezone", "America/New_York", "interval", "15m"), UUID.randomUUID());

        assertNotNull(sample.get("scheduledTime"));
        assertNotNull(sample.get("timestamp"));
        assertEquals("America/New_York", sample.get("timezone"));
        assertEquals("15m", sample.get("interval"));
    }

    @Test
    void webhookTriggerReturnsStructuredEventPayload() {
        Map<String, Object> sample = service.getTriggerSample("webhook", "incoming", null,
                Map.of(), UUID.randomUUID());

        assertNotNull(sample.get("id"));
        assertNotNull(sample.get("timestamp"));
        assertTrue(sample.get("body") instanceof Map<?, ?>);
        Map<?, ?> body = (Map<?, ?>) sample.get("body");
        assertEquals("item.created", body.get("event"));
        assertEquals("user@example.com", body.get("email"));
    }

    @Test
    void connectedAppWithResourceProviderReturnsRealSampleOption() {
        UUID connectionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(resourceFetchService.fetchResources(eq("slack"), eq("channels"), eq(connectionId), eq(userId), anyMap()))
                .thenReturn(List.of(new ResourceOption("C12345", "general", "Public channel")));

        Map<String, Object> sample = service.getTriggerSample("slack", "new-channel-message",
                connectionId.toString(), Map.of(), userId);

        assertEquals("C12345", sample.get("id"));
        assertEquals("general", sample.get("name"));
        assertEquals(Boolean.TRUE, sample.get("isRealSample"));
    }

    @Test
    void genericTriggerFallbackGeneratesContextualFields() {
        Map<String, Object> sample = service.getTriggerSample("gmail", "new-email", null,
                Map.of("label", "INBOX"), UUID.randomUUID());

        assertNotNull(sample.get("id"));
        assertEquals("sender@example.com", sample.get("from"));
        assertEquals("recipient@example.com", sample.get("to"));
        assertEquals("Sample Message Subject", sample.get("subject"));
    }
}
