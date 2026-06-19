package com.crescendo.apps.dropbox;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DropboxApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("dropbox", "Dropbox", "List, upload, download, share, search, and manage Dropbox files",
                "https://www.google.com/s2/favicons?domain=dropbox.com&sz=128", AuthType.OAUTH2,
                List.of(),
                List.of(
                        Map.of("actionKey", "list-folder", "name", "List Folder", "description", "List Dropbox folder contents",
                                "configSchema", List.of(Map.of("key", "path", "label", "Path", "type", "text", "required", false, "placeholder", "/"))),
                        Map.of("actionKey", "search", "name", "Search Files", "description", "Search files and folders",
                                "configSchema", List.of(
                                        Map.of("key", "query", "label", "Query", "type", "text", "required", true),
                                        Map.of("key", "path", "label", "Path", "type", "text", "required", false, "placeholder", "/"),
                                        Map.of("key", "maxResults", "label", "Max Results", "type", "text", "required", false, "placeholder", "25"))),
                        Map.of("actionKey", "upload-text", "name", "Upload Text", "description", "Upload text as a file",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Path", "type", "text", "required", true, "placeholder", "/notes/file.txt"),
                                        Map.of("key", "content", "label", "Content", "type", "textarea", "required", true))),
                        Map.of("actionKey", "download", "name", "Download File", "description", "Download file as Base64",
                                "configSchema", List.of(Map.of("key", "path", "label", "Path", "type", "text", "required", true))),
                        Map.of("actionKey", "delete", "name", "Delete File/Folder", "description", "Delete a Dropbox path",
                                "configSchema", List.of(Map.of("key", "path", "label", "Path", "type", "text", "required", true))),
                        Map.of("actionKey", "create-folder", "name", "Create Folder", "description", "Create a Dropbox folder",
                                "configSchema", List.of(Map.of("key", "path", "label", "Path", "type", "text", "required", true))),
                        Map.of("actionKey", "create-shared-link", "name", "Create Shared Link", "description", "Create or return a shared file/folder link",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Path", "type", "text", "required", true),
                                        Map.of("key", "audience", "label", "Audience", "type", "select", "required", false,
                                                "options", List.of(Map.of("value", "", "label", "Account default"), Map.of("value", "public", "label", "Public"), Map.of("value", "team", "label", "Team"))),
                                        Map.of("key", "access", "label", "Access", "type", "select", "required", false,
                                                "options", List.of(Map.of("value", "", "label", "Account default"), Map.of("value", "viewer", "label", "Viewer"), Map.of("value", "editor", "label", "Editor"))))),
                        Map.of("actionKey", "list-revisions", "name", "List Revisions", "description", "List file revisions",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Path", "type", "text", "required", true),
                                        Map.of("key", "limit", "label", "Limit", "type", "text", "required", false, "placeholder", "10"))),
                        Map.of("actionKey", "restore-revision", "name", "Restore Revision", "description", "Restore a file to a specific revision",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Path", "type", "text", "required", true),
                                        Map.of("key", "rev", "label", "Revision ID", "type", "text", "required", true)))
                )
        ).credentialSchema(List.of()).altAuthType(AuthType.OAUTH2)
                .category("file-storage")
                .helpUrl("https://www.dropbox.com/developers/documentation/http/documentation");
    }
}
