package com.crescendo.apps.microsoftteams;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MicrosoftTeamsApp implements AppDefinition {

    @Override
    public App toApp() {
        var teamField = Map.of("key", "teamId", "label", "Team",
                "type", "dynamic_dropdown", "resourceType", "teams",
                "required", true,
                "helpText", "Select the Microsoft Teams team");

        var channelField = Map.<String, Object>of("key", "channelId", "label", "Channel",
                "type", "dynamic_dropdown", "resourceType", "channels",
                "dependsOn", List.of("teamId"),
                "required", true,
                "helpText", "Select the channel within the team");

        return new App(
                "microsoft-teams",
                "Microsoft Teams",
                "Send messages, watch channels, and manage teams via Microsoft Graph",
                "https://www.google.com/s2/favicons?domain=teams.microsoft.com&sz=128",
                AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-channel-message",
                        "name", "New Channel Message",
                        "description", "Triggers when a new message is posted in a Teams channel",
                        "configSchema", List.of(teamField, channelField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "send-channel-message",
                        "name", "Send Channel Message",
                        "description", "Post a message to a Microsoft Teams channel",
                        "configSchema", List.of(
                            teamField, channelField,
                            Map.of("key", "message", "label", "Message",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hello from Crescendo!",
                                   "helpText", "Message content to send")
                        )
                    ),
                    Map.of(
                        "actionKey", "send-chat-message",
                        "name", "Send Chat Message",
                        "description", "Send a direct message to a user",
                        "configSchema", List.of(
                            Map.of("key", "userId", "label", "User",
                                   "type", "dynamic_dropdown", "resourceType", "members",
                                   "required", true,
                                   "helpText", "Select the user to message"),
                            Map.of("key", "message", "label", "Message",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hey!",
                                   "helpText", "Direct message content")
                        )
                    ),
                    Map.of(
                        "actionKey", "create-channel",
                        "name", "Create Channel",
                        "description", "Create a new channel in a team",
                        "configSchema", List.of(
                            teamField,
                            Map.of("key", "channelName", "label", "Channel Name",
                                   "type", "text", "required", true,
                                   "placeholder", "project-updates",
                                   "helpText", "Name for the new channel"),
                            Map.of("key", "description", "label", "Description",
                                   "type", "textarea", "required", false,
                                   "helpText", "Optional channel description")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("communication")
        .helpUrl("https://portal.azure.com/");
    }
}
