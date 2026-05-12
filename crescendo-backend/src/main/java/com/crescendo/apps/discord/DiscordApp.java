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

        return new App("discord", "Discord",
                "Send messages, watch channels, and manage servers in Discord",
                "/icons/discord.svg", AuthType.APIKEY,

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
                        "actionKey", "send-message",
                        "name", "Send Channel Message",
                        "description", "Post a message to a Discord channel",
                        "configSchema", List.of(
                            guildField, channelField,
                            Map.of("key", "content", "label", "Message Content",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hello from Crescendo!",
                                   "helpText", "The message content to send")
                        )
                    ),
                    Map.of(
                        "actionKey", "send-embed",
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
                        "actionKey", "send-dm",
                        "name", "Send Direct Message",
                        "description", "Send a private message to a user",
                        "configSchema", List.of(
                            guildField, userField,
                            Map.of("key", "content", "label", "Message",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hello!",
                                   "helpText", "The message to send privately")
                        )
                    ),
                    Map.of(
                        "actionKey", "create-channel",
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
                        "actionKey", "add-role",
                        "name", "Add Role to Member",
                        "description", "Assign a role to a server member",
                        "configSchema", List.of(guildField, userField, roleField)
                    ),
                    Map.of(
                        "actionKey", "remove-role",
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
