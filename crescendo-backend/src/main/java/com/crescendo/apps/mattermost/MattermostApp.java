package com.crescendo.apps.mattermost;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class MattermostApp implements AppDefinition {
        public App toApp() {
                return new App(
                                "mattermost",
                                "Mattermost",
                                """
                                                Mattermost is an open-source, self-hostable online chat service with file sharing, search, and integrations.

                                                **What you can do with Mattermost in Crescendo:**
                                                - Send automated alerts to specific channels on deployment success or failure
                                                - Create new discussion channels dynamically when major incidents occur
                                                - Manage channel memberships automatically based on directory changes

                                                **Actions available:**
                                                - Create Post — post a message to a channel
                                                - Delete Post — delete a post
                                                - Create Channel — create a new Mattermost channel
                                                - Add User to Channel — add a user to a channel

                                                **Who should use this:** DevOps teams, IT support, and organizations using Mattermost for internal communication.

                                                **Authentication:** Requires a Base URL and a Personal Access Token (API Key).
                                                """,
                                "https://upload.wikimedia.org/wikipedia/commons/9/91/Cib-mattermost_%28CoreUI_Icons_v1.0.0%29.svg",
                                AuthType.APIKEY,
                                List.of(),
                                List.of(
                                                Map.of(
                                                                "actionKey", "create-post",
                                                                "name", "Create Post",
                                                                "description", "Post a message to a channel",
                                                                "configSchema", List.of(
                                                                                Map.of("key", "channelId", "label",
                                                                                                "Channel ID", "type",
                                                                                                "text", "required",
                                                                                                true),
                                                                                Map.of("key", "message", "label",
                                                                                                "Message", "type",
                                                                                                "textarea", "required",
                                                                                                true))),
                                                Map.of(
                                                                "actionKey", "delete-post",
                                                                "name", "Delete Post",
                                                                "description", "Delete a post in Mattermost",
                                                                "configSchema", List.of(
                                                                                Map.of("key", "postId", "label",
                                                                                                "Post ID", "type",
                                                                                                "text", "required",
                                                                                                true))),
                                                Map.of(
                                                                "actionKey", "create-channel",
                                                                "name", "Create Channel",
                                                                "description", "Create a new Mattermost channel",
                                                                "configSchema", List.of(
                                                                                Map.of("key", "teamId", "label",
                                                                                                "Team ID", "type",
                                                                                                "text", "required",
                                                                                                true),
                                                                                Map.of("key", "name", "label",
                                                                                                "Channel Name (URL Safe)",
                                                                                                "type", "text",
                                                                                                "required", true),
                                                                                Map.of("key", "displayName", "label",
                                                                                                "Display Name", "type",
                                                                                                "text", "required",
                                                                                                true),
                                                                                Map.of("key", "type", "label", "Type",
                                                                                                "type", "dropdown",
                                                                                                "required", true,
                                                                                                "options",
                                                                                                List.of(Map.of("value",
                                                                                                                "O",
                                                                                                                "label",
                                                                                                                "Public (O)"),
                                                                                                                Map.of("value", "P",
                                                                                                                                "label",
                                                                                                                                "Private (P)"))))),
                                                Map.of(
                                                                "actionKey", "add-user-to-channel",
                                                                "name", "Add User to Channel",
                                                                "description", "Add a user to a channel",
                                                                "configSchema", List.of(
                                                                                Map.of("key", "channelId", "label",
                                                                                                "Channel ID", "type",
                                                                                                "text", "required",
                                                                                                true),
                                                                                Map.of("key", "userId", "label",
                                                                                                "User ID", "type",
                                                                                                "text", "required",
                                                                                                true)))))
                                .credentialSchema(List.of(
                                                Map.of("key", "baseUrl", "label", "Base URL", "type", "text",
                                                                "required", true),
                                                Map.of("key", "accessToken", "label", "Personal Access Token", "type",
                                                                "password", "required", true)))
                                .category("communication")
                                .helpUrl("https://avatars.githubusercontent.com/u/8966922?v=4");
        }
}
