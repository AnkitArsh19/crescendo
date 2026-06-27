package com.crescendo.apps.calcom;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Cal.com.
 *
 * Trigger events (from n8n Cal/CalTrigger.node.ts):
 *   - BOOKING_CREATED     : Triggered when a new Cal.com booking is made
 *   - BOOKING_CANCELLED   : Triggered when a Cal.com booking is cancelled
 *   - BOOKING_RESCHEDULED : Triggered when a Cal.com booking is rescheduled
 *   - MEETING_ENDED       : Triggered when a Cal.com meeting ends
 *
 * Authentication: API Key (calApi credential)
 */
@Component
public class CalComApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "calcom",
                "Cal.com",
                """
                Cal.com is an open-source scheduling platform that helps individuals and teams manage their bookings and availability.
                
                This integration supports event-based triggers via Cal.com's webhook system:
                - **Booking Created** (BOOKING_CREATED): Triggered when a new booking is made
                - **Booking Cancelled** (BOOKING_CANCELLED): Triggered when a booking is cancelled
                - **Booking Rescheduled** (BOOKING_RESCHEDULED): Triggered when a booking is rescheduled
                - **Meeting Ended** (MEETING_ENDED): Triggered when a meeting has ended
                
                Optionally filter by a specific event type ID or App ID.
                
                Authenticate using a Cal.com API Key.
                """,
                "https://www.google.com/s2/favicons?domain=cal.com&sz=128",
                AuthType.APIKEY,
                List.of(
                        Map.of(
                                "triggerKey", "calcom:booking:created",
                                "name", "Booking Created",
                                "description", "Receive notifications when a new Cal.com booking is created",
                                "configSchema", List.of(
                                        Map.of("key", "eventTypeId", "label", "Event Type ID (optional)", "type", "text")
                                )
                        ),
                        Map.of(
                                "triggerKey", "calcom:booking:cancelled",
                                "name", "Booking Cancelled",
                                "description", "Receive notifications when a Cal.com booking is cancelled",
                                "configSchema", List.of(
                                        Map.of("key", "eventTypeId", "label", "Event Type ID (optional)", "type", "text")
                                )
                        ),
                        Map.of(
                                "triggerKey", "calcom:booking:rescheduled",
                                "name", "Booking Rescheduled",
                                "description", "Receive notifications when a Cal.com booking is rescheduled",
                                "configSchema", List.of(
                                        Map.of("key", "eventTypeId", "label", "Event Type ID (optional)", "type", "text")
                                )
                        ),
                        Map.of(
                                "triggerKey", "calcom:meeting:ended",
                                "name", "Meeting Ended",
                                "description", "Receive notifications when a Cal.com meeting has ended",
                                "configSchema", List.of(
                                        Map.of("key", "eventTypeId", "label", "Event Type ID (optional)", "type", "text")
                                )
                        )
                ),
                List.of()
        ).credentialSchema(List.of(
                Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true),
                Map.of("key", "apiVersion", "label", "API Version (1 = before v2.0, 2 = v2.0+)", "type", "text", "default", "2")
        )).category("scheduling");
    }
}
