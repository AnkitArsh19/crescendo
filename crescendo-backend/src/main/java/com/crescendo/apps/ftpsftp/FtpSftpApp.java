package com.crescendo.apps.ftpsftp;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FtpSftpApp implements AppDefinition {
    public App toApp() {
        return new App(
                "ftp-sftp",
                "FTP / SFTP", """
                The FTP / SFTP app allows Crescendo to connect securely to remote file servers, enabling automated file transfers and directory management.

                **What you can do with FTP/SFTP in Crescendo:**
                - Automatically download daily CSV reports from a legacy vendor's FTP server
                - Upload generated invoice PDFs securely via SFTP to a client's server
                - Sync files between an AWS S3 bucket and an on-premise SFTP server
                - List directory contents to trigger workflows when new files arrive

                **Actions available:**
                - List Directory — view files and folders on the remote server
                - Download File — retrieve file content into your workflow as Base64
                - Upload File — send Base64 data to the remote server
                - Delete File — delete a file from the remote server
                - Rename File — rename or move a file on the remote server

                **Who should use this:** Data engineers, enterprise architects, and teams integrating with legacy systems.

                **Authentication:** Protocol (FTP/SFTP), Host, Port, Username, Password, or Private Key.
                """,
                "/icons/ftp.png",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "list",
                                "name", "List Directory",
                                "description", "List remote files",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Path", "type", "text", "required", false, "placeholder", "/"),
                                        Map.of("key", "recursive", "label", "Recursive", "type", "boolean", "required", false, "helpText", "List files and folders recursively"),
                                        Map.of("key", "timeout", "label", "Timeout (ms)", "type", "number", "required", false, "placeholder", "10000")
                                )
                        ),
                        Map.of(
                                "actionKey", "download",
                                "name", "Download File",
                                "description", "Download file as Base64",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Remote Path", "type", "text", "required", true),
                                        Map.of("key", "enableConcurrentReads", "label", "Enable Concurrent Reads", "type", "boolean", "required", false),
                                        Map.of("key", "maxConcurrentReads", "label", "Max Concurrent Reads", "type", "number", "required", false, "placeholder", "5"),
                                        Map.of("key", "chunkSize", "label", "Chunk Size (KB)", "type", "number", "required", false, "placeholder", "64"),
                                        Map.of("key", "timeout", "label", "Timeout (ms)", "type", "number", "required", false, "placeholder", "10000")
                                )
                        ),
                        Map.of(
                                "actionKey", "upload",
                                "name", "Upload File",
                                "description", "Upload Base64 data",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Remote Path", "type", "text", "required", true),
                                        Map.of("key", "binaryData", "label", "Binary Data", "type", "boolean", "required", false, "helpText", "Upload a Base64 string instead of plain text"),
                                        Map.of("key", "fileContent", "label", "File Content", "type", "textarea", "required", false, "helpText", "Plain text to upload (if not using binary data)"),
                                        Map.of("key", "base64", "label", "Base64 Data", "type", "textarea", "required", false, "helpText", "Base64 string to upload (if using binary data)"),
                                        Map.of("key", "timeout", "label", "Timeout (ms)", "type", "number", "required", false, "placeholder", "10000")
                                )
                        ),
                        Map.of(
                                "actionKey", "delete",
                                "name", "Delete File",
                                "description", "Delete a file on the remote server",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Remote Path", "type", "text", "required", true),
                                        Map.of("key", "folder", "label", "Folder", "type", "boolean", "required", false, "helpText", "Whether target is a folder"),
                                        Map.of("key", "recursive", "label", "Recursive", "type", "boolean", "required", false, "helpText", "Remove all files and directories recursively"),
                                        Map.of("key", "timeout", "label", "Timeout (ms)", "type", "number", "required", false, "placeholder", "10000")
                                )
                        ),
                        Map.of(
                                "actionKey", "rename",
                                "name", "Rename File",
                                "description", "Rename a file on the remote server",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Remote Path", "type", "text", "required", true),
                                        Map.of("key", "newPath", "label", "New Remote Path", "type", "text", "required", true),
                                        Map.of("key", "createDirectories", "label", "Create Directories", "type", "boolean", "required", false, "helpText", "Recursively create destination directory"),
                                        Map.of("key", "timeout", "label", "Timeout (ms)", "type", "number", "required", false, "placeholder", "10000")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of(
                        "key", "protocol",
                        "label", "Protocol",
                        "type", "select",
                        "required", true,
                        "options", List.of(
                                Map.of("value", "ftp", "label", "FTP"),
                                Map.of("value", "sftp", "label", "SFTP")
                        )
                ),
                Map.of("key", "host", "label", "Host", "type", "text", "required", true),
                Map.of("key", "port", "label", "Port", "type", "text", "required", false),
                Map.of("key", "username", "label", "Username", "type", "text", "required", true),
                Map.of("key", "password", "label", "Password", "type", "password", "required", false),
                Map.of("key", "privateKey", "label", "Private Key", "type", "textarea", "required", false)
        )).category("developer").helpUrl("");
    }
}
