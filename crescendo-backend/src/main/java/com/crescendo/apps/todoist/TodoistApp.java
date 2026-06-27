package com.crescendo.apps.todoist;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Todoist.
 *
 * Resources (from n8n Todoist node):
 *   - comment  : create, delete, get, getAll, update
 *   - label    : create, delete, get, getAll, update
 *   - project  : archive, create, delete, get, getCollaborators, getAll, unarchive, update
 *   - reminder : create, delete, getAll, update
 *   - section  : create, delete, get, getAll, update
 *   - task     : close, create, delete, get, getAll, move, quickAdd, reopen, update
 */
@Component
public class TodoistApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "todoist",
                "Todoist",
                """
                Todoist is a cloud-based service that lets you manage your tasks and projects.
                
                This integration provides:
                - **Task**: Close, Create, Delete, Get, Get All, Move, Quick Add, Reopen, Update
                - **Project**: Archive, Create, Delete, Get, Get Collaborators, Get All, Unarchive, Update
                - **Section**: Create, Delete, Get, Get All, Update
                - **Comment**: Create, Delete, Get, Get All, Update
                - **Label**: Create, Delete, Get, Get All, Update
                - **Reminder**: Create, Delete, Get All, Update
                
                Authenticate using a Todoist API Key or OAuth2.
                """,
                "https://www.google.com/s2/favicons?domain=todoist.com&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // COMMENT
                        Map.of("actionKey", "todoist:comment:create", "name", "Create Comment", "description", "Create a comment", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text"), Map.of("key", "projectId", "label", "Project ID", "type", "text"), Map.of("key", "content", "label", "Content", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:comment:delete", "name", "Delete Comment", "description", "Delete a comment", "configSchema", List.of(Map.of("key", "commentId", "label", "Comment ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:comment:get", "name", "Get Comment", "description", "Get a comment", "configSchema", List.of(Map.of("key", "commentId", "label", "Comment ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:comment:getAll", "name", "Get All Comments", "description", "Get all comments", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text"), Map.of("key", "projectId", "label", "Project ID", "type", "text"))),
                        Map.of("actionKey", "todoist:comment:update", "name", "Update Comment", "description", "Update a comment", "configSchema", List.of(Map.of("key", "commentId", "label", "Comment ID", "type", "text", "required", true), Map.of("key", "content", "label", "Content", "type", "text", "required", true))),

                        // LABEL
                        Map.of("actionKey", "todoist:label:create", "name", "Create Label", "description", "Create a label", "configSchema", List.of(Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:label:delete", "name", "Delete Label", "description", "Delete a label", "configSchema", List.of(Map.of("key", "labelId", "label", "Label ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:label:get", "name", "Get Label", "description", "Get a label", "configSchema", List.of(Map.of("key", "labelId", "label", "Label ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:label:getAll", "name", "Get All Labels", "description", "Get all labels", "configSchema", List.of()),
                        Map.of("actionKey", "todoist:label:update", "name", "Update Label", "description", "Update a label", "configSchema", List.of(Map.of("key", "labelId", "label", "Label ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text"))),

                        // PROJECT
                        Map.of("actionKey", "todoist:project:archive", "name", "Archive Project", "description", "Archive a project (requires Sync API)", "configSchema", List.of(Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:project:create", "name", "Create Project", "description", "Create a project", "configSchema", List.of(Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:project:delete", "name", "Delete Project", "description", "Delete a project", "configSchema", List.of(Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:project:get", "name", "Get Project", "description", "Get a project", "configSchema", List.of(Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:project:getCollaborators", "name", "Get Collaborators", "description", "Get project collaborators", "configSchema", List.of(Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:project:getAll", "name", "Get All Projects", "description", "Get all projects", "configSchema", List.of()),
                        Map.of("actionKey", "todoist:project:unarchive", "name", "Unarchive Project", "description", "Unarchive a project (requires Sync API)", "configSchema", List.of(Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:project:update", "name", "Update Project", "description", "Update a project", "configSchema", List.of(Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),

                        // REMINDER (Sync API)
                        Map.of("actionKey", "todoist:reminder:create", "name", "Create Reminder", "description", "Create a reminder", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true), Map.of("key", "minuteOffset", "label", "Minute Offset", "type", "number"))),
                        Map.of("actionKey", "todoist:reminder:delete", "name", "Delete Reminder", "description", "Delete a reminder", "configSchema", List.of(Map.of("key", "reminderId", "label", "Reminder ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:reminder:getAll", "name", "Get All Reminders", "description", "Get all reminders", "configSchema", List.of()),
                        Map.of("actionKey", "todoist:reminder:update", "name", "Update Reminder", "description", "Update a reminder", "configSchema", List.of(Map.of("key", "reminderId", "label", "Reminder ID", "type", "text", "required", true), Map.of("key", "minuteOffset", "label", "Minute Offset", "type", "number"))),

                        // SECTION
                        Map.of("actionKey", "todoist:section:create", "name", "Create Section", "description", "Create a section", "configSchema", List.of(Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:section:delete", "name", "Delete Section", "description", "Delete a section", "configSchema", List.of(Map.of("key", "sectionId", "label", "Section ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:section:get", "name", "Get Section", "description", "Get a section", "configSchema", List.of(Map.of("key", "sectionId", "label", "Section ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:section:getAll", "name", "Get All Sections", "description", "Get all sections", "configSchema", List.of(Map.of("key", "projectId", "label", "Project ID", "type", "text"))),
                        Map.of("actionKey", "todoist:section:update", "name", "Update Section", "description", "Update a section", "configSchema", List.of(Map.of("key", "sectionId", "label", "Section ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),

                        // TASK
                        Map.of("actionKey", "todoist:task:close", "name", "Close Task", "description", "Close a task", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:task:create", "name", "Create Task", "description", "Create a task", "configSchema", List.of(Map.of("key", "content", "label", "Content", "type", "text", "required", true), Map.of("key", "projectId", "label", "Project ID", "type", "text"), Map.of("key", "sectionId", "label", "Section ID", "type", "text"), Map.of("key", "description", "label", "Description", "type", "text"))),
                        Map.of("actionKey", "todoist:task:delete", "name", "Delete Task", "description", "Delete a task", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:task:get", "name", "Get Task", "description", "Get a task", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:task:getAll", "name", "Get All Tasks", "description", "Get all tasks", "configSchema", List.of(Map.of("key", "projectId", "label", "Project ID", "type", "text"))),
                        Map.of("actionKey", "todoist:task:move", "name", "Move Task", "description", "Move a task (Sync API)", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true), Map.of("key", "projectId", "label", "Project ID", "type", "text"))),
                        Map.of("actionKey", "todoist:task:quickAdd", "name", "Quick Add Task", "description", "Quick add a task", "configSchema", List.of(Map.of("key", "text", "label", "Text", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:task:reopen", "name", "Reopen Task", "description", "Reopen a task", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true))),
                        Map.of("actionKey", "todoist:task:update", "name", "Update Task", "description", "Update a task", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true), Map.of("key", "content", "label", "Content", "type", "text")))
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true)
        )).altAuthType(AuthType.OAUTH2).category("task-management");
    }
}
