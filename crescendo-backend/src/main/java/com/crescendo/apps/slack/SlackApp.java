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

        return new App("slack", "Slack", """
                Slack is the leading team communication platform used by millions of organizations worldwide. It organizes conversations into channels, supports direct messaging, file sharing, and integrates with hundreds of apps.

                **What you can do with Slack in Crescendo:**
                - Send messages to any channel or user automatically
                - Post announcements, alerts, or daily digests
                - Get notified via trigger when a message is posted, someone is mentioned, or a reaction is added
                - Create new channels, invite users, and set channel topics programmatically
                - Search messages across your workspace

                **Triggers available:**
                - New Message in Channel — fire a workflow whenever a message is posted
                - New Mention — react when your bot or keyword is mentioned
                - New Reaction — catch emoji reactions on messages
                - New Channel Created — trigger when channels are created
                - New User Joined — onboard new workspace members automatically

                **Who should use this:** Engineering teams for incident alerts, HR teams for onboarding flows, marketing teams for campaign notifications, and anyone who wants to receive automated reports or approvals in Slack.

                **Authentication:** OAuth 2.0 (connect your Slack account) or Bot Token / API Key (paste a bot token from api.slack.com/apps — best for server-side bots and webhook-style automation).
                """,
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
                        "actionKey", "sendMessage",
                        "name", "Send Channel Message",
                        "description", "Post a message to a Slack channel",
                        "configSchema", List.of(
                            channelField,
                            Map.of("key", "text", "label", "Message Text",
                                   "type", "textarea", "required", false,
                                   "placeholder", "Hello from Crescendo!",
                                   "helpText", "The message content to send (required if blocksUi is empty)"),
                            Map.of("key", "blocksUi", "label", "Blocks (JSON)",
                                   "type", "textarea", "required", false,
                                   "placeholder", "[{\"type\": \"section\", \"text\": {\"type\": \"mrkdwn\", \"text\": \"Hello\"}}]",
                                   "helpText", "Advanced Slack Blocks UI JSON"),
                            Map.of("key", "thread_ts", "label", "Thread Timestamp",
                                   "type", "text", "required", false,
                                   "placeholder", "1663233118.856619",
                                   "helpText", "Provide a message timestamp to reply in its thread"),
                            Map.of("key", "replyBroadcast", "label", "Also Send to Channel",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False")),
                                   "helpText", "If replying to a thread, broadcast to the channel"),
                            Map.of("key", "ephemeralUserId", "label", "Ephemeral User ID",
                                   "type", "text", "required", false,
                                   "helpText", "If provided, sends message to the channel but only visible to this user"),
                            Map.of("key", "icon_emoji", "label", "Icon Emoji",
                                   "type", "text", "required", false,
                                   "placeholder", ":ghost:",
                                   "helpText", "Override bot icon (requires chat:write.customize)"),
                            Map.of("key", "icon_url", "label", "Icon URL",
                                   "type", "text", "required", false,
                                   "helpText", "Override bot avatar URL (requires chat:write.customize)"),
                            Map.of("key", "username", "label", "Bot Username",
                                   "type", "text", "required", false,
                                   "placeholder", "My Custom Bot",
                                   "helpText", "Override bot name (requires chat:write.customize)")
                        )
                    ),
                    Map.of(
                        "actionKey", "sendDirectMessage",
                        "name", "Send Direct Message",
                        "description", "Send a direct message to a user",
                        "configSchema", List.of(
                            userField,
                            Map.of("key", "text", "label", "Message Text",
                                   "type", "textarea", "required", false,
                                   "placeholder", "Hello!",
                                   "helpText", "The message content to send (required if blocksUi is empty)"),
                            Map.of("key", "blocksUi", "label", "Blocks (JSON)",
                                   "type", "textarea", "required", false,
                                   "helpText", "Advanced Slack Blocks UI JSON"),
                            Map.of("key", "thread_ts", "label", "Thread Timestamp",
                                   "type", "text", "required", false,
                                   "helpText", "Provide a message timestamp to reply in its thread")
                        )
                    ),
                    Map.of(
                        "actionKey", "updateMessage",
                        "name", "Update Message",
                        "description", "Update an existing message in Slack",
                        "configSchema", List.of(
                            channelField,
                            Map.of("key", "ts", "label", "Message Timestamp",
                                   "type", "text", "required", true,
                                   "helpText", "The timestamp of the message to update"),
                            Map.of("key", "text", "label", "New Text",
                                   "type", "textarea", "required", false,
                                   "helpText", "The updated text (required if blocksUi is empty)"),
                            Map.of("key", "blocksUi", "label", "New Blocks (JSON)",
                                   "type", "textarea", "required", false,
                                   "helpText", "The updated Blocks JSON array")
                        )
                    ),
                    Map.of(
                        "actionKey", "deleteMessage",
                        "name", "Delete Message",
                        "description", "Delete a message in Slack",
                        "configSchema", List.of(
                            channelField,
                            Map.of("key", "ts", "label", "Message Timestamp",
                                   "type", "text", "required", true,
                                   "helpText", "The timestamp of the message to delete")
                        )
                    ),
                    Map.of(
                        "actionKey", "createChannel",
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
                        "actionKey", "setTopic",
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
                        "actionKey", "addReaction",
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
                        "actionKey", "searchMessages",
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
                        "actionKey", "inviteToChannel",
                        "name", "Invite User to Channel",
                        "description", "Invite a user to join a channel",
                        "configSchema", List.of(channelField, userField)
                    )
                )
        )
        .altAuthType(AuthType.OAUTH2)
        .credentialSchema(List.of(
            Map.of("key", "botToken", "label", "Bot Token",
                    "type", "password", "required", true,
                    "placeholder", "xoxb-...",
                    "helpText", "Create an app at api.slack.com and copy its Bot User OAuth Token.",
                    "authOption", "APIKEY"),
            Map.of("key", "accessToken", "label", "OAuth Access Token",
                    "type", "oauth", "required", true,
                    "helpText", "Sign in with Slack to authorize user-level actions.",
                    "authOption", "OAUTH2")
        ))
        .category("communication")
        .helpUrl("https://api.slack.com/apps");
    }
}
