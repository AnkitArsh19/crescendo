package com.crescendo.apps.discord;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DiscordApp implements AppDefinition {

    @Override
    public App toApp() {
        var guildField = Map.of("key", "guildId", "label", "Server",
                "type", "dynamic_dropdown", "resourceType", "guilds",
                "required", true,
                "helpText", "Select the Discord server");

        var channelField = Map.<String, Object>of("key", "channelId", "label", "Channel",
                "type", "dynamic_dropdown", "resourceType", "channels",
                "dependsOn", List.of("guildId"),
                "required", true,
                "helpText", "Select the text channel");

        var roleField = Map.<String, Object>of("key", "roleId", "label", "Role",
                "type", "dynamic_dropdown", "resourceType", "roles",
                "dependsOn", List.of("guildId"),
                "required", true,
                "helpText", "Select the role");

        var userField = Map.<String, Object>of("key", "userId", "label", "User",
                "type", "dynamic_dropdown", "resourceType", "members",
                "dependsOn", List.of("guildId"),
                "required", true,
                "helpText", "Select the user");

        return new App("discord", "Discord", """
                Discord is the easiest way to talk over voice, video, and text. The Crescendo Discord app connects your servers to external workflows.

                **What you can do with Discord in Crescendo:**
                - Send messages and rich embeds to channels automatically
                - Notify your team of new GitHub issues, Notion pages, or support tickets
                - Automatically assign or remove roles when users take actions elsewhere
                - Direct message users for personal alerts

                **Triggers available:**
                - New Message — trigger a workflow when a message is posted
                - New Member — welcome new users to the server
                - New Reaction — kick off a process when a specific emoji is added

                **Who should use this:** Community managers, support teams, and developer squads who use Discord as their primary operations hub.

                **Authentication:** Bot Token (for background bot actions) or OAuth 2.0 (for user-level channel discovery).
                """,
                "https://www.google.com/s2/favicons?domain=discord.com&sz=128", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-message",
                        "name", "New Message in Channel",
                        "description", "Triggers when a new message is posted in a channel",
                        "configSchema", List.of(guildField, channelField)
                    ),
                    Map.of(
                        "triggerKey", "new-member",
                        "name", "New Member Joined",
                        "description", "Triggers when a new member joins the server",
                        "configSchema", List.of(guildField)
                    ),
                    Map.of(
                        "triggerKey", "new-reaction",
                        "name", "New Reaction Added",
                        "description", "Triggers when a reaction is added to a message",
                        "configSchema", List.of(
                            guildField, channelField,
                            Map.of("key", "emojiFilter", "label", "Emoji Filter",
                                   "type", "text", "required", false,
                                   "placeholder", "👍",
                                   "helpText", "Optionally filter by specific emoji")
                        )
                    ),
                    Map.of(
                        "triggerKey", "member-role-changed",
                        "name", "Member Role Changed",
                        "description", "Triggers when a member's roles are updated",
                        "configSchema", List.of(guildField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "sendMessage",
                        "name", "Send Channel Message",
                        "description", "Post a message to a Discord channel",
                        "configSchema", List.of(
                            guildField, channelField,
                            Map.of("key", "content", "label", "Message Content",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hello from Crescendo!",
                                   "helpText", "The message content to send"),
                            Map.of("key", "tts", "label", "Text-to-Speech (TTS)",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False")),
                                   "helpText", "Whether to read the message aloud"),
                            Map.of("key", "message_reference", "label", "Reply to Message ID",
                                   "type", "text", "required", false,
                                   "helpText", "Fill this to make your message a reply to an existing message ID"),
                            Map.of("key", "flags", "label", "Flags",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(
                                       Map.of("value", "4", "label", "Suppress Embeds"),
                                       Map.of("value", "4096", "label", "Suppress Notifications")
                                   ),
                                   "helpText", "Optional message flags")
                        )
                    ),
                    Map.of(
                        "actionKey", "sendEmbed",
                        "name", "Send Embed Message",
                        "description", "Send a rich embed message to a channel",
                        "configSchema", List.of(
                            guildField, channelField,
                            Map.of("key", "title", "label", "Embed Title",
                                   "type", "text", "required", true,
                                   "helpText", "Title of the embed"),
                            Map.of("key", "description", "label", "Embed Description",
                                   "type", "textarea", "required", false,
                                   "helpText", "Description text in the embed"),
                            Map.of("key", "color", "label", "Color",
                                   "type", "text", "required", false,
                                   "placeholder", "#5865F2",
                                   "helpText", "Hex color for the embed sidebar")
                        )
                    ),
                    Map.of(
                        "actionKey", "sendDirectMessage",
                        "name", "Send Direct Message",
                        "description", "Send a private message to a user",
                        "configSchema", List.of(
                            guildField, userField,
                            Map.of("key", "content", "label", "Message",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hello!",
                                   "helpText", "The message to send privately"),
                            Map.of("key", "tts", "label", "Text-to-Speech (TTS)",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "message_reference", "label", "Reply to Message ID",
                                   "type", "text", "required", false),
                            Map.of("key", "flags", "label", "Flags",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(
                                       Map.of("value", "4", "label", "Suppress Embeds"),
                                       Map.of("value", "4096", "label", "Suppress Notifications")
                                   ))
                        )
                    ),
                    Map.of(
                        "actionKey", "deleteMessage",
                        "name", "Delete Message",
                        "description", "Delete a message in a channel",
                        "configSchema", List.of(
                            guildField, channelField,
                            Map.of("key", "messageId", "label", "Message ID",
                                   "type", "text", "required", true,
                                   "helpText", "The ID of the message to delete")
                        )
                    ),
                    Map.of(
                        "actionKey", "addReaction",
                        "name", "Add Reaction",
                        "description", "Add an emoji reaction to a message",
                        "configSchema", List.of(
                            guildField, channelField,
                            Map.of("key", "messageId", "label", "Message ID",
                                   "type", "text", "required", true,
                                   "helpText", "The ID of the message to react to"),
                            Map.of("key", "emoji", "label", "Emoji",
                                   "type", "text", "required", true,
                                   "placeholder", "👍",
                                   "helpText", "The emoji character or custom emoji (name:id) to react with")
                        )
                    ),
                    Map.of(
                        "actionKey", "createChannel",
                        "name", "Create Channel",
                        "description", "Create a new text channel in a server",
                        "configSchema", List.of(
                            guildField,
                            Map.of("key", "channelName", "label", "Channel Name",
                                   "type", "text", "required", true,
                                   "placeholder", "project-updates",
                                   "helpText", "Name for the new channel"),
                            Map.of("key", "topic", "label", "Topic",
                                   "type", "text", "required", false,
                                   "helpText", "Optional channel topic")
                        )
                    ),
                    Map.of(
                        "actionKey", "addRole",
                        "name", "Add Role to Member",
                        "description", "Assign a role to a server member",
                        "configSchema", List.of(guildField, userField, roleField)
                    ),
                    Map.of(
                        "actionKey", "removeRole",
                        "name", "Remove Role from Member",
                        "description", "Remove a role from a server member",
                        "configSchema", List.of(guildField, userField, roleField)
                    )
                )
        )
        .altAuthType(AuthType.OAUTH2)
        .credentialSchema(List.of(
            Map.of("key", "botToken", "label", "Bot Token",
                    "type", "password", "required", true,
                    "placeholder", "MTIzNDU2Nzg5MDEy...",
                    "helpText", "Create a bot in the Discord Developer Portal and copy its token.",
                    "authOption", "APIKEY"),
            Map.of("key", "accessToken", "label", "OAuth Access Token",
                    "type", "oauth", "required", true,
                    "helpText", "Sign in with Discord to authorize user-level actions.",
                    "authOption", "OAUTH2")
        ))
        .category("communication")
        .helpUrl("https://discord.com/developers/applications");
    }
}
