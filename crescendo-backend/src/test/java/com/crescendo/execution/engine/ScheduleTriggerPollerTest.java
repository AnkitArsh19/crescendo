package com.crescendo.execution.engine;

import com.crescendo.apps.schedule.ScheduleTriggerPoller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleTriggerPollerTest {

    private final ScheduleTriggerPoller poller = new ScheduleTriggerPoller();

    @Test
    @DisplayName("ScheduleTriggerPoller supports schedule appKey")
    void supports_returnsTrueForSchedule() {
        assertTrue(poller.supports("schedule", "every-minute"));
        assertFalse(poller.supports("slack", "new-message"));
    }

    @Test
    @DisplayName("ScheduleTriggerPoller evaluates cron expression and emits payload when execution falls in window")
    void poll_evaluatesCronExpression() {
        Instant lastPoll = Instant.now().minus(2, ChronoUnit.HOURS);
        Map<String, Object> config = Map.of("cronExpression", "0 * * * * *"); // every minute

        List<Map<String, Object>> events = poller.poll(Map.of(), config, lastPoll);

        assertFalse(events.isEmpty());
        Map<String, Object> event = events.getFirst();
        assertEquals("cron", event.get("_type"));
        assertEquals("0 * * * * *", event.get("_rule"));
        assertNotNull(event.get("timestamp"));
        assertNotNull(event.get("Readable date"));
    }

    @Test
    @DisplayName("ScheduleTriggerPoller evaluates minute interval and emits payload")
    void poll_evaluatesMinuteInterval() {
        Instant lastPoll = Instant.now().minus(10, ChronoUnit.MINUTES);
        Map<String, Object> config = Map.of("interval", 5, "unit", "minutes");

        List<Map<String, Object>> events = poller.poll(Map.of(), config, lastPoll);

        assertFalse(events.isEmpty());
        Map<String, Object> event = events.getFirst();
        assertEquals("interval", event.get("_type"));
        assertEquals("5 minutes", event.get("_rule"));
    }
}
