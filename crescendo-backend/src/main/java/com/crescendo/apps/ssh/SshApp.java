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
                "SSH",
                "Run commands on a server using the local ssh client",
                "/icons/ssh.svg",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "run-command",
                                "name", "Run Command",
                                "description", "Run a remote shell command",
                                "configSchema", List.of(
                                        Map.of("key", "command", "label", "Command", "type", "textarea", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "host", "label", "Host", "type", "text", "required", true),
                Map.of("key", "username", "label", "Username", "type", "text", "required", true),
                Map.of("key", "port", "label", "Port", "type", "text", "required", false, "placeholder", "22"),
                Map.of("key", "identityFile", "label", "Identity File", "type", "text", "required", false)
        )).category("developer").helpUrl("https://www.openssh.com/");
    }
}
