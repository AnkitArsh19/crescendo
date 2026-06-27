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
                "Microsoft Teams", """
                Microsoft Teams is a proprietary business communication platform. The Crescendo Teams app allows you to send messages, watch channels, and manage teams automatically via Microsoft Graph.

                **What you can do with Teams in Crescendo:**
                - Post an alert in a channel when a critical Jira issue is created
                - Send a direct chat message to an employee when their HR onboarding is complete
                - Automatically create a new channel for every new Salesforce Opportunity
                - Forward high-priority emails from Outlook directly to a team channel

                **Triggers available:**
                - New Channel Message — trigger a workflow when a message is posted

                **Actions available:**
                - Send Channel Message — post to a shared channel
                - Send Chat Message — DM a specific user
                - Create Channel — add a new channel to a team

                **Who should use this:** Enterprise organizations, IT administrators, and corporate teams using the Microsoft ecosystem for internal communication.

                **Authentication:** OAuth 2.0 (connect your Microsoft account).
                """,
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
                        "actionKey", "sendChannelMessage",
                        "name", "Send Channel Message",
                        "description", "Post a message to a Microsoft Teams channel",
                        "configSchema", List.of(
                            teamField, channelField,
                            Map.of("key", "contentType", "label", "Content Type",
                                   "type", "dropdown", "required", true,
                                   "options", List.of(Map.of("value", "html", "label", "HTML"), Map.of("value", "text", "label", "Text")),
                                   "helpText", "Whether the message is plain text or HTML"),
                            Map.of("key", "message", "label", "Message",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hello from Crescendo!",
                                   "helpText", "Message content to send"),
                            Map.of("key", "replyToId", "label", "Reply to ID",
                                   "type", "text", "required", false,
                                   "helpText", "Optional ID of the message you want to reply to")
                        )
                    ),
                    Map.of(
                        "actionKey", "sendChatMessage",
                        "name", "Send Chat Message",
                        "description", "Send a direct message to a user",
                        "configSchema", List.of(
                            Map.of("key", "userId", "label", "User",
                                   "type", "dynamic_dropdown", "resourceType", "members",
                                   "required", true,
                                   "helpText", "Select the user to message"),
                            Map.of("key", "contentType", "label", "Content Type",
                                   "type", "dropdown", "required", true,
                                   "options", List.of(Map.of("value", "html", "label", "HTML"), Map.of("value", "text", "label", "Text")),
                                   "helpText", "Whether the message is plain text or HTML"),
                            Map.of("key", "message", "label", "Message",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hey!",
                                   "helpText", "Direct message content")
                        )
                    ),
                    Map.of(
                        "actionKey", "createChannel",
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
