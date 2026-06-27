package com.crescendo.apps.ssh;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SshApp implements AppDefinition {
    public App toApp() {
        return new App(
                "ssh",
                "SSH", """
                SSH (Secure Shell) is a network protocol that provides a secure way to access a computer over an unsecured network. The Crescendo SSH app allows you to run shell commands on remote servers safely.

                **What you can do with SSH in Crescendo:**
                - Automatically restart a remote web server if an HTTP health check fails
                - Run a database cleanup script via cron schedule every weekend
                - Fetch the last 100 lines of a log file when a critical alert is triggered
                - Trigger deployment scripts (like Docker Compose) upon receiving a GitHub webhook

                **Actions available:**
                - Run Command — execute a shell command on the remote server and return its stdout/stderr output

                **Who should use this:** Sysadmins, DevOps engineers, and self-hosters automating remote infrastructure management.

                **Authentication:** Server details (Host, Port, Username) and either a Password or Private Key.
                """,
                "/icons/ssh.svg",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "run-command",
                                "name", "Run Command",
                                "description", "Run a remote shell command",
                                "configSchema", List.of(
                                        Map.of("key", "command", "label", "Command", "type", "textarea", "required", true),
                                        Map.of("key", "cwd", "label", "Working Directory", "type", "text", "required", false, "placeholder", "/", "helpText", "Directory to execute the command in")
                                )
                        ),
                        Map.of(
                                "actionKey", "download",
                                "name", "Download File",
                                "description", "Download a file via SFTP over SSH",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Remote Path", "type", "text", "required", true, "placeholder", "/home/user/file.txt"),
                                        Map.of("key", "fileName", "label", "File Name Override", "type", "text", "required", false, "helpText", "Overrides the binary data file name")
                                )
                        ),
                        Map.of(
                                "actionKey", "upload",
                                "name", "Upload File",
                                "description", "Upload a file via SFTP over SSH",
                                "configSchema", List.of(
                                        Map.of("key", "path", "label", "Remote Directory", "type", "text", "required", true, "placeholder", "/home/user"),
                                        Map.of("key", "base64", "label", "Base64 Data", "type", "textarea", "required", true),
                                        Map.of("key", "fileName", "label", "File Name", "type", "text", "required", false, "helpText", "File name to save as")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "host", "label", "Host", "type", "text", "required", true),
                Map.of("key", "username", "label", "Username", "type", "text", "required", true),
                Map.of("key", "port", "label", "Port", "type", "text", "required", false, "placeholder", "22"),
                Map.of("key", "password", "label", "Password", "type", "password", "required", false),
                Map.of("key", "privateKey", "label", "Private Key", "type", "textarea", "required", false)
        )).category("developer").helpUrl("https://www.openssh.com/");
    }
}
