package com.crescendo.apps.slack;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SlackApp implements AppDefinition {

    @Override
    public App toApp() {
        var channelField = Map.of("key", "channel", "label", "Channel",
                "type", "dynamic_dropdown", "resourceType", "channels",
                "required", true,
                "helpText", "Select the Slack channel");

        var userField = Map.of("key", "userId", "label", "User",
                "type", "dynamic_dropdown", "resourceType", "users",
                "required", true,
                "helpText", "Select the user");

        return new App("slack", "Slack", "Send messages, manage channels, and automate Slack workflows",
                "https://www.google.com/s2/favicons?domain=slack.com&sz=128", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-message",
                        "name", "New Message in Channel",
                        "description", "Triggers when a new message is posted",
                        "configSchema", List.of(channelField)
                    ),
                    Map.of(
                        "triggerKey", "new-mention",
                        "name", "New Mention",
                        "description", "Triggers when the bot is mentioned in a channel",
                        "configSchema", List.of(channelField)
                    ),
                    Map.of(
                        "triggerKey", "new-reaction",
                        "name", "New Reaction Added",
                        "description", "Triggers when a reaction is added to a message",
                        "configSchema", List.of(
                            channelField,
                            Map.of("key", "emojiFilter", "label", "Emoji Filter",
                                   "type", "text", "required", false,
                                   "placeholder", "thumbsup",
                                   "helpText", "Optionally filter by emoji name")
                        )
                    ),
                    Map.of(
                        "triggerKey", "new-channel",
                        "name", "New Channel Created",
                        "description", "Triggers when a new channel is created in the workspace",
                        "configSchema", List.of()
                    ),
                    Map.of(
                        "triggerKey", "new-user",
                        "name", "New User Joined",
                        "description", "Triggers when a new user joins the workspace",
                        "configSchema", List.of()
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "send-message",
                        "name", "Send Channel Message",
                        "description", "Post a message to a Slack channel",
                        "configSchema", List.of(
                            channelField,
                            Map.of("key", "text", "label", "Message Text",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hello from Crescendo!",
                                   "helpText", "The message content to send")
                        )
                    ),
                    Map.of(
                        "actionKey", "send-dm",
                        "name", "Send Direct Message",
                        "description", "Send a direct message to a user",
                        "configSchema", List.of(
                            userField,
                            Map.of("key", "text", "label", "Message Text",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hello!",
                                   "helpText", "The message content to send")
                        )
                    ),
                    Map.of(
                        "actionKey", "create-channel",
                        "name", "Create Channel",
                        "description", "Create a new Slack channel",
                        "configSchema", List.of(
                            Map.of("key", "channelName", "label", "Channel Name",
                                   "type", "text", "required", true,
                                   "placeholder", "project-updates",
                                   "helpText", "Name for the new channel"),
                            Map.of("key", "isPrivate", "label", "Private Channel?",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(
                                       Map.of("value", "false", "label", "Public (default)"),
                                       Map.of("value", "true", "label", "Private")
                                   ),
                                   "helpText", "Whether the channel should be private")
                        )
                    ),
                    Map.of(
                        "actionKey", "set-channel-topic",
                        "name", "Set Channel Topic",
                        "description", "Set or update a channel's topic",
                        "configSchema", List.of(
                            channelField,
                            Map.of("key", "topic", "label", "Topic",
                                   "type", "text", "required", true,
                                   "placeholder", "Sprint 14 — Ship date: April 1",
                                   "helpText", "New channel topic text")
                        )
                    ),
                    Map.of(
                        "actionKey", "add-reaction",
                        "name", "Add Reaction",
                        "description", "Add an emoji reaction to a message",
                        "configSchema", List.of(
                            channelField,
                            Map.of("key", "timestamp", "label", "Message Timestamp",
                                   "type", "text", "required", true,
                                   "helpText", "The timestamp of the message (from a trigger)"),
                            Map.of("key", "emoji", "label", "Emoji",
                                   "type", "text", "required", true,
                                   "placeholder", "thumbsup",
                                   "helpText", "Emoji name (without colons)")
                        )
                    ),
                    Map.of(
                        "actionKey", "find-message",
                        "name", "Find Message",
                        "description", "Search for messages matching a query",
                        "configSchema", List.of(
                            Map.of("key", "query", "label", "Search Query",
                                   "type", "text", "required", true,
                                   "placeholder", "from:@alice project launch",
                                   "helpText", "Slack search query"),
                            Map.of("key", "maxResults", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "5",
                                   "helpText", "Maximum messages to return")
                        )
                    ),
                    Map.of(
                        "actionKey", "invite-to-channel",
                        "name", "Invite User to Channel",
                        "description", "Invite a user to join a channel",
                        "configSchema", List.of(channelField, userField)
                    )
                )
        )
        .credentialSchema(List.of()).altAuthType(AuthType.APIKEY)
        .category("communication")
        .helpUrl("https://api.slack.com/apps");
    }
}
