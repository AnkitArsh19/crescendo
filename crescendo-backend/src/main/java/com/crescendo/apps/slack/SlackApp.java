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

        return new App("slack", "Slack", "Send messages, manage channels, and automate Slack workflows",
                "/icons/slack.svg", AuthType.OAUTH2,

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
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "send-message",
                        "name", "Send Message",
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
                            Map.of("key", "userId", "label", "User",
                                   "type", "dynamic_dropdown", "resourceType", "users",
                                   "required", true,
                                   "helpText", "Select the user to message"),
                            Map.of("key", "text", "label", "Message Text",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hello!",
                                   "helpText", "The message content to send")
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
                    )
                )
        )
        .credentialSchema(List.of(
            Map.of("key", "botToken", "label", "Bot User OAuth Token",
                    "type", "password", "required", true,
                    "placeholder", "xoxb-...",
                    "helpText", "Install your Slack app and copy the Bot User OAuth Token",
                    "authOption", "APIKEY")
        ))
        .altAuthType(AuthType.APIKEY)
        .category("communication")
        .helpUrl("https://api.slack.com/apps");
    }
}
