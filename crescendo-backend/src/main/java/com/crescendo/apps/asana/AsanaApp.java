package com.crescendo.apps.asana;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Asana.
 *
 * Resources (from n8n Asana.node.ts):
 *   - project     : create, getAll
 *   - subtask     : create, getAll
 *   - task        : create, delete, get, getAll, move, search, update
 *   - taskComment : add, remove
 *   - taskProject : add, remove
 *   - taskTag     : add, remove
 *   - user        : get, getAll
 *
 * Authentication: Personal Access Token (accessToken) or OAuth2
 */
@Component
public class AsanaApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "asana",
                "Asana",
                """
                Asana is a web and mobile work management platform designed to help teams organize, track, and manage their work.
                
                This integration mirrors n8n's Asana node and provides:
                - **Project**: Create, Get All
                - **Subtask**: Create, Get All
                - **Task**: Create, Delete, Get, Get All, Move, Search, Update
                - **Task Comment**: Add, Remove
                - **Task Project**: Add, Remove
                - **Task Tag**: Add, Remove
                - **User**: Get, Get All
                
                Authenticate with a Personal Access Token or OAuth2.
                """,
                "https://www.google.com/s2/favicons?domain=asana.com&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // PROJECT
                        Map.of("actionKey", "asana:project:create", "name", "Create Project", "description", "Create a project", "configSchema", List.of(
                                Map.of("key", "name", "label", "Name", "type", "text", "required", true),
                                Map.of("key", "workspaceId", "label", "Workspace ID", "type", "text", "required", true),
                                Map.of("key", "teamId", "label", "Team ID", "type", "text"),
                                Map.of("key", "additionalFields", "label", "Additional Fields (JSON)", "type", "json")
                        )),
                        Map.of("actionKey", "asana:project:getAll", "name", "Get All Projects", "description", "Get all projects", "configSchema", List.of(
                                Map.of("key", "workspaceId", "label", "Workspace ID", "type", "text"),
                                Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100)
                        )),

                        // SUBTASK
                        Map.of("actionKey", "asana:subtask:create", "name", "Create Subtask", "description", "Create a subtask", "configSchema", List.of(
                                Map.of("key", "taskId", "label", "Parent Task ID", "type", "text", "required", true),
                                Map.of("key", "name", "label", "Name", "type", "text", "required", true),
                                Map.of("key", "additionalFields", "label", "Additional Fields (JSON)", "type", "json")
                        )),
                        Map.of("actionKey", "asana:subtask:getAll", "name", "Get All Subtasks", "description", "Get all subtasks", "configSchema", List.of(
                                Map.of("key", "taskId", "label", "Parent Task ID", "type", "text", "required", true)
                        )),

                        // TASK
                        Map.of("actionKey", "asana:task:create", "name", "Create Task", "description", "Create a task", "configSchema", List.of(
                                Map.of("key", "name", "label", "Name", "type", "text", "required", true),
                                Map.of("key", "workspaceId", "label", "Workspace ID", "type", "text"),
                                Map.of("key", "projectId", "label", "Project ID", "type", "text"),
                                Map.of("key", "dueOn", "label", "Due On (YYYY-MM-DD)", "type", "text"),
                                Map.of("key", "assignee", "label", "Assignee (GID)", "type", "text"),
                                Map.of("key", "notes", "label", "Notes / Description", "type", "text"),
                                Map.of("key", "additionalFields", "label", "Additional Fields (JSON)", "type", "json")
                        )),
                        Map.of("actionKey", "asana:task:delete", "name", "Delete Task", "description", "Delete a task", "configSchema", List.of(
                                Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "asana:task:get", "name", "Get Task", "description", "Get a task", "configSchema", List.of(
                                Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "asana:task:getAll", "name", "Get All Tasks", "description", "Get all tasks", "configSchema", List.of(
                                Map.of("key", "projectId", "label", "Project ID", "type", "text"),
                                Map.of("key", "workspaceId", "label", "Workspace ID", "type", "text"),
                                Map.of("key", "assignee", "label", "Assignee (GID)", "type", "text"),
                                Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100)
                        )),
                        Map.of("actionKey", "asana:task:move", "name", "Move Task", "description", "Move a task between sections", "configSchema", List.of(
                                Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true),
                                Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", true),
                                Map.of("key", "sectionId", "label", "Section ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "asana:task:search", "name", "Search Tasks", "description", "Search for tasks in a workspace", "configSchema", List.of(
                                Map.of("key", "workspaceId", "label", "Workspace ID", "type", "text", "required", true),
                                Map.of("key", "text", "label", "Search Text", "type", "text"),
                                Map.of("key", "additionalFields", "label", "Filters (JSON)", "type", "json")
                        )),
                        Map.of("actionKey", "asana:task:update", "name", "Update Task", "description", "Update a task", "configSchema", List.of(
                                Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true),
                                Map.of("key", "additionalFields", "label", "Fields to update (JSON)", "type", "json", "required", true)
                        )),

                        // TASK COMMENT
                        Map.of("actionKey", "asana:taskComment:add", "name", "Add Comment to Task", "description", "Add a comment to a task", "configSchema", List.of(
                                Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true),
                                Map.of("key", "text", "label", "Comment Text", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "asana:taskComment:remove", "name", "Remove Comment from Task", "description", "Remove a comment from a task", "configSchema", List.of(
                                Map.of("key", "commentId", "label", "Comment ID", "type", "text", "required", true)
                        )),

                        // TASK PROJECT
                        Map.of("actionKey", "asana:taskProject:add", "name", "Add Task to Project", "description", "Add a task to a project", "configSchema", List.of(
                                Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true),
                                Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "asana:taskProject:remove", "name", "Remove Task from Project", "description", "Remove a task from a project", "configSchema", List.of(
                                Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true),
                                Map.of("key", "projectId", "label", "Project ID", "type", "text", "required", true)
                        )),

                        // TASK TAG
                        Map.of("actionKey", "asana:taskTag:add", "name", "Add Tag to Task", "description", "Add a tag to a task", "configSchema", List.of(
                                Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true),
                                Map.of("key", "tagId", "label", "Tag ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "asana:taskTag:remove", "name", "Remove Tag from Task", "description", "Remove a tag from a task", "configSchema", List.of(
                                Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true),
                                Map.of("key", "tagId", "label", "Tag ID", "type", "text", "required", true)
                        )),

                        // USER
                        Map.of("actionKey", "asana:user:get", "name", "Get User", "description", "Get a user", "configSchema", List.of(
                                Map.of("key", "userId", "label", "User ID or 'me'", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "asana:user:getAll", "name", "Get All Users", "description", "Get all users in a workspace", "configSchema", List.of(
                                Map.of("key", "workspaceId", "label", "Workspace ID", "type", "text"),
                                Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100)
                        ))
                )
        ).credentialSchema(List.of(
                Map.of("key", "accessToken", "label", "Personal Access Token", "type", "password", "required", true)
        )).altAuthType(AuthType.OAUTH2).category("task-management");
    }
}
