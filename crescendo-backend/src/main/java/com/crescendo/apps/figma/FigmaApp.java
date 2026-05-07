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
        return new App("figma", "Figma", "Monitor file changes and export design assets",
                "/icons/figma.svg", AuthType.OAUTH2,
                List.of(Map.of(
                    "triggerKey", "file-updated",
                    "name", "File Updated",
                    "description", "Triggers when a Figma file is modified"
                )),
                List.of(
                    Map.of(
                        "actionKey", "get-file",
                        "name", "Get File",
                        "description", "Retrieve Figma file metadata and structure",
                        "configSchema", List.of(
                            Map.of("key", "fileKey", "label", "Figma File URL or Key",
                                   "type", "text", "required", true,
                                   "placeholder", "https://www.figma.com/design/ABC123xyz/My-Design",
                                   "helpText", "Paste a Figma file URL or just the file key")
                        )
                    ),
                    Map.of(
                        "actionKey", "post-comment",
                        "name", "Post Comment",
                        "description", "Add a comment to a Figma file",
                        "configSchema", List.of(
                            Map.of("key", "fileKey", "label", "Figma File URL or Key",
                                   "type", "text", "required", true,
                                   "placeholder", "https://www.figma.com/design/ABC123xyz/My-Design",
                                   "helpText", "Paste a Figma file URL or just the file key"),
                            Map.of("key", "message", "label", "Comment",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Looks good! Ship it 🚀",
                                   "helpText", "The comment text to post on the file")
                        )
                    ),
                    Map.of(
                        "actionKey", "list-comments",
                        "name", "List Comments",
                        "description", "List all comments on a Figma file",
                        "configSchema", List.of(
                            Map.of("key", "fileKey", "label", "Figma File URL or Key",
                                   "type", "text", "required", true,
                                   "placeholder", "https://www.figma.com/design/ABC123xyz/My-Design",
                                   "helpText", "Paste a Figma file URL or just the file key")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("https://www.figma.com/developers/api");
    }
}
