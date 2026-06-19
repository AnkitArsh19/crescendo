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
                "FTP / SFTP",
                "Transfer files over FTP or SFTP",
                "/icons/ftp.svg",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "list",
                                "name", "List Directory",
                                "description", "List remote files",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Path", "type", "text", "required", false, "placeholder", "/")
                                )
                        ),
                        Map.of(
                                "actionKey", "download",
                                "name", "Download File",
                                "description", "Download file as Base64",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Remote Path", "type", "text", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "upload",
                                "name", "Upload File",
                                "description", "Upload Base64 data",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Remote Path", "type", "text", "required", true),
                                        Map.of("key", "base64", "label", "Base64 Data", "type", "textarea", "required", true)
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
