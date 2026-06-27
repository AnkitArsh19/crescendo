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
                "Jenkins", """
                Jenkins is an open-source automation server that helps build, test, and deploy software reliably. The Crescendo Jenkins app allows you to trigger CI/CD pipelines from external events.

                **What you can do with Jenkins in Crescendo:**
                - Trigger a deployment build when a specific release label is applied to a GitHub Pull Request
                - Pause a workflow using the Approval app, then trigger a Jenkins job only if the manager approves
                - Monitor the status of a long-running test suite and notify a Slack channel upon completion
                - Pass dynamic variables from an incoming webhook directly into a Jenkins parameterized build

                **Actions available:**
                - Trigger Build — start a Jenkins job execution, optionally passing dynamic JSON parameters
                - Get Job — retrieve metadata and current status about a specific Jenkins job

                **Who should use this:** DevOps engineers, Release Managers, and QA automation teams orchestrating deployment pipelines.

                **Authentication:** Jenkins credentials (Base URL, Username, and API Token).
                """,
                "https://www.google.com/s2/favicons?domain=jenkins.io&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "jenkins:build:trigger",
                                "name", "Trigger Build",
                                "description", "Trigger a Jenkins job build",
                                "configSchema", List.of(
                                        Map.of("key", "jobPath", "label", "Job Path", "type", "text", "required", true, "placeholder", "folder/job-name"),
                                        Map.of("key", "parameters", "label", "Parameters (JSON)", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "jenkins:build:get",
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
