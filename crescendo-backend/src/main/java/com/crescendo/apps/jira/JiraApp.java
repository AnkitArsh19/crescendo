package com.crescendo.apps.jira;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Jira Software Cloud.
 *
 * Resources (from n8n Jira node):
 *   - issue           : changelog, create, delete, get, getAll, notify, update
 *   - issueAttachment : add, get, getAll, remove
 *   - issueComment    : add, get, getAll, remove, update
 *   - user            : create, delete, get
 */
@Component
public class JiraApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "jira",
                "Jira Software",
                """
                Jira Software is an agile project management tool that supports any agile methodology, be it scrum, kanban, or your own unique flavor.
                
                This integration supports Jira Software Cloud and provides:
                - **Issue**: Changelog, Create, Delete, Get, Get All, Notify, Update
                - **Issue Attachment**: Add, Get, Get All, Remove
                - **Issue Comment**: Add, Get, Get All, Remove, Update
                - **User**: Create, Delete, Get
                
                Authenticate using an Atlassian API Token and your Atlassian account Email. You must also provide your Jira Cloud domain.
                """,
                "https://www.google.com/s2/favicons?domain=atlassian.net&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // ISSUE
                        Map.of("actionKey", "jira:issue:create", "name", "Create Issue", "description", "Create an issue", "configSchema", List.of(
                                Map.of("key", "projectKey", "label", "Project Key", "type", "text", "required", true),
                                Map.of("key", "issueType", "label", "Issue Type ID", "type", "text", "required", true),
                                Map.of("key", "summary", "label", "Summary", "type", "text", "required", true),
                                Map.of("key", "additionalFields", "label", "Additional Fields (JSON)", "type", "json")
                        )),
                        Map.of("actionKey", "jira:issue:update", "name", "Update Issue", "description", "Update an issue", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true),
                                Map.of("key", "updateFields", "label", "Update Fields (JSON)", "type", "json", "required", true)
                        )),
                        Map.of("actionKey", "jira:issue:delete", "name", "Delete Issue", "description", "Delete an issue", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:issue:get", "name", "Get Issue", "description", "Get an issue", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:issue:getAll", "name", "Get All Issues (Search)", "description", "Search issues using JQL", "configSchema", List.of(
                                Map.of("key", "jql", "label", "JQL Query", "type", "text"),
                                Map.of("key", "limit", "label", "Limit", "type", "number", "default", 50)
                        )),
                        Map.of("actionKey", "jira:issue:changelog", "name", "Get Issue Changelog", "description", "Get changelog of an issue", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:issue:notify", "name", "Notify Issue", "description", "Send a notification for an issue", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true),
                                Map.of("key", "body", "label", "Notification Body (JSON)", "type", "json", "required", true)
                        )),

                        // ISSUE ATTACHMENT
                        Map.of("actionKey", "jira:issueAttachment:add", "name", "Add Attachment", "description", "Add attachment to an issue", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true),
                                Map.of("key", "fileName", "label", "File Name", "type", "text", "required", true),
                                Map.of("key", "fileContent", "label", "File Content (Base64)", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:issueAttachment:get", "name", "Get Attachment", "description", "Get an attachment metadata", "configSchema", List.of(
                                Map.of("key", "attachmentId", "label", "Attachment ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:issueAttachment:getAll", "name", "Get All Attachments", "description", "Get all attachments for an issue", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:issueAttachment:remove", "name", "Remove Attachment", "description", "Remove an attachment", "configSchema", List.of(
                                Map.of("key", "attachmentId", "label", "Attachment ID", "type", "text", "required", true)
                        )),

                        // ISSUE COMMENT
                        Map.of("actionKey", "jira:issueComment:add", "name", "Add Comment", "description", "Add comment to an issue", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true),
                                Map.of("key", "body", "label", "Comment Body", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:issueComment:get", "name", "Get Comment", "description", "Get a comment", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true),
                                Map.of("key", "commentId", "label", "Comment ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:issueComment:getAll", "name", "Get All Comments", "description", "Get all comments for an issue", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:issueComment:update", "name", "Update Comment", "description", "Update an issue comment", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true),
                                Map.of("key", "commentId", "label", "Comment ID", "type", "text", "required", true),
                                Map.of("key", "body", "label", "Comment Body", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:issueComment:remove", "name", "Remove Comment", "description", "Delete an issue comment", "configSchema", List.of(
                                Map.of("key", "issueKey", "label", "Issue Key", "type", "text", "required", true),
                                Map.of("key", "commentId", "label", "Comment ID", "type", "text", "required", true)
                        )),

                        // USER
                        Map.of("actionKey", "jira:user:create", "name", "Create User", "description", "Create a user", "configSchema", List.of(
                                Map.of("key", "email", "label", "Email Address", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:user:get", "name", "Get User", "description", "Get a user", "configSchema", List.of(
                                Map.of("key", "accountId", "label", "Account ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "jira:user:delete", "name", "Delete User", "description", "Delete a user", "configSchema", List.of(
                                Map.of("key", "accountId", "label", "Account ID", "type", "text", "required", true)
                        ))
                )
        ).credentialSchema(List.of(
                Map.of("key", "domain", "label", "Jira Domain", "type", "text", "required", true, "placeholder", "https://your-domain.atlassian.net"),
                Map.of("key", "email", "label", "Email Address", "type", "text", "required", true),
                Map.of("key", "apiToken", "label", "API Token", "type", "password", "required", true)
        )).altAuthType(AuthType.OAUTH2).category("task-management");
    }
}
