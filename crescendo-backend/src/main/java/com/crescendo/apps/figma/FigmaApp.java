package com.crescendo.apps.figma;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class FigmaApp implements AppDefinition {
    @Override
    public App toApp() {
        var fileField = Map.of("key", "fileKey", "label", "Figma File URL or Key",
                "type", "text", "required", true,
                "placeholder", "https://www.figma.com/design/ABC123xyz/My-Design",
                "helpText", "Paste a Figma file URL or just the file key");

        return new App("figma", "Figma", """
                Figma is a collaborative web application for interface design. The Crescendo Figma app allows you to monitor files, export assets, and track design changes in real-time.

                **What you can do with Figma in Crescendo:**
                - Notify Slack when a Figma file is updated
                - Export design assets and upload them directly to Google Drive
                - Track new comments on design files and sync them to Linear
                - Generate a weekly report of design changes

                **Actions available:**
                - Get File — retrieve metadata and node information
                - Get Comments — fetch the latest comments on a design
                - Export Image — generate and download rendered images of specific nodes

                **Who should use this:** Design teams coordinating with developers, project managers tracking design progress, and agencies sharing assets with clients.

                **Authentication:** OAuth 2.0 (connect your Figma account) or Personal Access Token.
                """,
                "https://www.google.com/s2/favicons?domain=figma.com&sz=128", AuthType.OAUTH2,
                List.of(
                    Map.of("triggerKey", "file-updated", "name", "File Updated",
                        "description", "Triggers when a Figma file is modified",
                        "configSchema", List.of(fileField)),
                    Map.of("triggerKey", "new-comment", "name", "New Comment",
                        "description", "Triggers when a comment is added to a file",
                        "configSchema", List.of(fileField)),
                    Map.of("triggerKey", "new-component", "name", "New Component",
                        "description", "Triggers when a new component is created",
                        "configSchema", List.of(fileField))
                ),
                List.of(
                    Map.of("actionKey", "get-file", "name", "Get File",
                        "description", "Retrieve file metadata and structure",
                        "configSchema", List.of(fileField)),
                    Map.of("actionKey", "post-comment", "name", "Post Comment",
                        "description", "Add a comment to a Figma file",
                        "configSchema", List.of(fileField,
                            Map.of("key", "message", "label", "Comment", "type", "textarea", "required", true,
                                   "placeholder", "Looks good! Ship it", "helpText", "Comment text"))),
                    Map.of("actionKey", "list-comments", "name", "List Comments",
                        "description", "List all comments on a file",
                        "configSchema", List.of(fileField)),
                    Map.of("actionKey", "export-file", "name", "Export File",
                        "description", "Export file pages as images",
                        "configSchema", List.of(fileField,
                            Map.of("key", "nodeIds", "label", "Node IDs", "type", "text", "required", false,
                                   "placeholder", "0:1,1:2", "helpText", "Comma-separated node IDs (leave empty for all)"),
                            Map.of("key", "format", "label", "Format", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "png", "label", "PNG"),
                                       Map.of("value", "svg", "label", "SVG"),
                                       Map.of("value", "pdf", "label", "PDF"),
                                       Map.of("value", "jpg", "label", "JPG")
                                   ), "helpText", "Export format"),
                            Map.of("key", "scale", "label", "Scale", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "1", "label", "1x"),
                                       Map.of("value", "2", "label", "2x"),
                                       Map.of("value", "3", "label", "3x"),
                                       Map.of("value", "4", "label", "4x")
                                   ), "helpText", "Export scale")))
                )
        ).credentialSchema(List.of()).category("productivity").helpUrl("https://www.figma.com/developers/api");
    }
}
