package com.crescendo.apps.calendly;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Calendly.
 *
 * Trigger events (from n8n CalendlyTriggerV2.node.ts):
 *   - invitee.created  : Receive notifications when a new Calendly event is created
 *   - invitee.canceled : Receive notifications when a Calendly event is canceled
 *
 * Scope options: user | organization
 *
 * Authentication: OAuth2 (calendlyOAuth2Api)
 */
@Component
public class CalendlyApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "calendly",
                "Calendly",
                """
                Calendly is an automated scheduling platform that removes the back-and-forth of scheduling meetings.
                
                This integration supports event-based triggers via Calendly's webhook subscriptions API:
                - **Event Created** (invitee.created): Triggered when a new Calendly event is booked
                - **Event Canceled** (invitee.canceled): Triggered when a Calendly event is canceled
                
                Configure scope to trigger on events for the current user or the entire organization.
                
                Authenticate using OAuth2 (Calendly OAuth2 API credentials).
                """,
                "https://www.google.com/s2/favicons?domain=calendly.com&sz=128",
                AuthType.OAUTH2,
                List.of(
                        Map.of(
                                "triggerKey", "calendly:invitee:created",
                                "name", "Event Created",
                                "description", "Receive notifications when a new Calendly event is created",
                                "configSchema", List.of(
                                        Map.of("key", "scope", "label", "Scope (user or organization)", "type", "text", "default", "user")
                                )
                        ),
                        Map.of(
                                "triggerKey", "calendly:invitee:canceled",
                                "name", "Event Canceled",
                                "description", "Receive notifications when a Calendly event is canceled",
                                "configSchema", List.of(
                                        Map.of("key", "scope", "label", "Scope (user or organization)", "type", "text", "default", "user")
                                )
                        )
                ),
                List.of()
        ).credentialSchema(List.of(
                Map.of("key", "accessToken", "label", "OAuth2 Access Token", "type", "password", "required", true)
        )).category("scheduling");
    }
}
