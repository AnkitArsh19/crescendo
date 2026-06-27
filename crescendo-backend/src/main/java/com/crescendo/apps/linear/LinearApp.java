package com.crescendo.apps.linear;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Linear.
 *
 * Resources:
 *   - issue: create, delete, get, getAll, update, addLink
 *   - comment: addComment
 */
@Component
public class LinearApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "linear",
                "Linear",
                """
                Linear is an issue tracking tool built for speed.
                
                This integration provides operations for:
                - **Issue**: Create, Delete, Get, Get All, Update, Add Link
                - **Comment**: Add Comment
                
                Authenticate using a Linear API Key or OAuth2.
                """,
                "https://www.google.com/s2/favicons?domain=linear.app&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // ISSUE
                        Map.of("actionKey", "linear:issue:create", "name", "Create Issue", "description", "Create an issue", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true), Map.of("key", "title", "label", "Title", "type", "text", "required", true), Map.of("key", "description", "label", "Description", "type", "text"), Map.of("key", "assigneeId", "label", "Assignee ID", "type", "text"), Map.of("key", "stateId", "label", "State ID", "type", "text"))),
                        Map.of("actionKey", "linear:issue:delete", "name", "Delete Issue", "description", "Delete an issue", "configSchema", List.of(Map.of("key", "issueId", "label", "Issue ID", "type", "text", "required", true))),
                        Map.of("actionKey", "linear:issue:get", "name", "Get Issue", "description", "Get an issue", "configSchema", List.of(Map.of("key", "issueId", "label", "Issue ID", "type", "text", "required", true))),
                        Map.of("actionKey", "linear:issue:getAll", "name", "Get All Issues", "description", "Get all issues", "configSchema", List.of()),
                        Map.of("actionKey", "linear:issue:update", "name", "Update Issue", "description", "Update an issue", "configSchema", List.of(Map.of("key", "issueId", "label", "Issue ID", "type", "text", "required", true), Map.of("key", "title", "label", "Title", "type", "text"), Map.of("key", "description", "label", "Description", "type", "text"), Map.of("key", "assigneeId", "label", "Assignee ID", "type", "text"), Map.of("key", "stateId", "label", "State ID", "type", "text"))),
                        Map.of("actionKey", "linear:issue:addLink", "name", "Add Link", "description", "Add a link to an issue", "configSchema", List.of(Map.of("key", "issueId", "label", "Issue ID", "type", "text", "required", true), Map.of("key", "link", "label", "Link URL", "type", "text", "required", true))),

                        // COMMENT
                        Map.of("actionKey", "linear:comment:addComment", "name", "Add Comment", "description", "Add a comment to an issue", "configSchema", List.of(Map.of("key", "issueId", "label", "Issue ID", "type", "text", "required", true), Map.of("key", "comment", "label", "Comment", "type", "text", "required", true), Map.of("key", "parentId", "label", "Parent Comment ID", "type", "text")))
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiToken", "label", "API Key", "type", "password", "required", true)
        )).altAuthType(AuthType.OAUTH2).category("task-management");
    }
}
