package com.crescendo.apps.homeassistant;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HomeAssistantApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("home-assistant", "Home Assistant", """
                Home Assistant is an open-source home automation system that puts local control and privacy first. The Crescendo Home Assistant app allows you to trigger physical devices from your digital workflows.

                **What you can do with Home Assistant in Crescendo:**
                - Flash your smart lights red when a high-priority Jira bug is assigned to you
                - Turn on your coffee maker automatically 10 minutes before your first Google Calendar meeting
                - Query the current temperature from a smart thermostat and log it to a spreadsheet
                - Lock the front door automatically when you set your Slack status to "Away"

                **Actions available:**
                - Call Service — trigger any Home Assistant service (e.g., light.turn_on)
                - Get State — retrieve the current status of an entity

                **Who should use this:** IoT enthusiasts, remote workers, and self-hosters bridging the gap between web services and physical hardware.

                **Authentication:** Server URL and Long-Lived Access Token.
                """,
                "https://www.google.com/s2/favicons?domain=home-assistant.io&sz=128", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "homeassistant:state:get", "name", "Get Entity State",
                                "description", "Read the state for a Home Assistant entity",
                                "configSchema", List.of(
                                        Map.of("key", "entityId", "label", "Entity ID", "type", "text", "required", true,
                                                "placeholder", "light.living_room"))),
                        Map.of("actionKey", "homeassistant:service:call", "name", "Call Service",
                                "description", "Call a Home Assistant service",
                                "configSchema", List.of(
                                        Map.of("key", "domain", "label", "Domain", "type", "text", "required", true,
                                                "placeholder", "light"),
                                        Map.of("key", "service", "label", "Service", "type", "text", "required", true,
                                                "placeholder", "turn_on"),
                                        Map.of("key", "data", "label", "Service Data (JSON)", "type", "json", "required", false,
                                                "placeholder", "{\"entity_id\":\"light.living_room\"}")))
                )
        ).credentialSchema(List.of(
                Map.of("key", "baseUrl", "label", "Base URL", "type", "text", "required", true,
                        "placeholder", "http://homeassistant.local:8123"),
                Map.of("key", "accessToken", "label", "Long-Lived Access Token", "type", "password", "required", true)
        )).category("iot").helpUrl("https://developers.home-assistant.io/docs/api/rest/");
    }
}
