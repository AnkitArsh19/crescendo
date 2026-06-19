package com.crescendo.apps.jenkins;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class JenkinsApp implements AppDefinition {
    public App toApp() {
        return new App(
                "jenkins",
                "Jenkins",
                "Trigger and inspect Jenkins jobs",
                "/icons/jenkins.svg",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "trigger-build",
                                "name", "Trigger Build",
                                "description", "Trigger a Jenkins job build",
                                "configSchema", List.of(
                                        Map.of("key", "jobPath", "label", "Job Path", "type", "text", "required", true, "placeholder", "folder/job-name"),
                                        Map.of("key", "parameters", "label", "Parameters (JSON)", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "get-job",
                                "name", "Get Job",
                                "description", "Fetch Jenkins job metadata",
                                "configSchema", List.of(
                                        Map.of("key", "jobPath", "label", "Job Path", "type", "text", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "baseUrl", "label", "Base URL", "type", "text", "required", true),
                Map.of("key", "username", "label", "Username", "type", "text", "required", true),
                Map.of("key", "apiToken", "label", "API Token", "type", "password", "required", true)
        )).category("developer").helpUrl("https://www.jenkins.io/doc/book/using/remote-access-api/");
    }
}
