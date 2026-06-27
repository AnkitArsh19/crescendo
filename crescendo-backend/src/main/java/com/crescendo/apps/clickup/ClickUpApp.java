package com.crescendo.apps.clickup;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for ClickUp.
 */
@Component
public class ClickUpApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "clickup",
                "ClickUp",
                """
                ClickUp is a cloud-based collaboration and project management tool suitable for businesses of all sizes and industries.
                
                This integration provides comprehensive operations for:
                - **Task**: Create, Delete, Get, Get All, Update
                - **Task Dependency**: Create, Delete
                - **Task List**: Remove
                - **Task Tag**: Remove
                - **List**: Create, Delete, Get, Get All, Update
                - **Folder**: Create, Delete, Get, Get All, Update
                - **Goal**: Create, Delete, Get, Get All, Update
                - **Goal Key Result**: Create, Delete, Update
                - **Guest**: Create, Delete, Get, Update
                - **Comment**: Create, Delete, Get All, Update
                - **Checklist**: Create, Delete, Update
                - **Checklist Item**: Create, Delete, Update
                - **Space Tag**: Create, Delete, Get All, Update
                - **Time Entry**: Create, Delete, Get, Get All, Update
                - **Time Entry Tag**: Get All, Remove
                
                Authenticate using a ClickUp API Token or OAuth2.
                """,
                "https://www.google.com/s2/favicons?domain=clickup.com&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // TASK
                        Map.of("actionKey", "clickup:task:create", "name", "Create Task", "description", "Create a task", "configSchema", List.of(Map.of("key", "listId", "label", "List ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:task:delete", "name", "Delete Task", "description", "Delete a task", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:task:get", "name", "Get Task", "description", "Get a task", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:task:getAll", "name", "Get All Tasks", "description", "Get all tasks", "configSchema", List.of(Map.of("key", "listId", "label", "List ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:task:update", "name", "Update Task", "description", "Update a task", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text"))),

                        // TASK DEPENDENCY
                        Map.of("actionKey", "clickup:taskDependency:create", "name", "Create Dependency", "description", "Create a task dependency", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true), Map.of("key", "dependsOnTaskId", "label", "Depends On Task ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:taskDependency:delete", "name", "Delete Dependency", "description", "Delete a task dependency", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true), Map.of("key", "dependsOnTaskId", "label", "Depends On Task ID", "type", "text", "required", true))),

                        // TASK LIST / TASK TAG
                        Map.of("actionKey", "clickup:taskList:remove", "name", "Remove Task from List", "description", "Remove task from a list", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true), Map.of("key", "listId", "label", "List ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:taskTag:remove", "name", "Remove Tag from Task", "description", "Remove tag from task", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true), Map.of("key", "tagName", "label", "Tag Name", "type", "text", "required", true))),

                        // LIST
                        Map.of("actionKey", "clickup:list:create", "name", "Create List", "description", "Create a list", "configSchema", List.of(Map.of("key", "folderId", "label", "Folder ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:list:delete", "name", "Delete List", "description", "Delete a list", "configSchema", List.of(Map.of("key", "listId", "label", "List ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:list:get", "name", "Get List", "description", "Get a list", "configSchema", List.of(Map.of("key", "listId", "label", "List ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:list:getAll", "name", "Get All Lists", "description", "Get all lists in a folder", "configSchema", List.of(Map.of("key", "folderId", "label", "Folder ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:list:update", "name", "Update List", "description", "Update a list", "configSchema", List.of(Map.of("key", "listId", "label", "List ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text"))),

                        // FOLDER
                        Map.of("actionKey", "clickup:folder:create", "name", "Create Folder", "description", "Create a folder", "configSchema", List.of(Map.of("key", "spaceId", "label", "Space ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:folder:delete", "name", "Delete Folder", "description", "Delete a folder", "configSchema", List.of(Map.of("key", "folderId", "label", "Folder ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:folder:get", "name", "Get Folder", "description", "Get a folder", "configSchema", List.of(Map.of("key", "folderId", "label", "Folder ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:folder:getAll", "name", "Get All Folders", "description", "Get all folders in a space", "configSchema", List.of(Map.of("key", "spaceId", "label", "Space ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:folder:update", "name", "Update Folder", "description", "Update a folder", "configSchema", List.of(Map.of("key", "folderId", "label", "Folder ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text"))),

                        // GOAL
                        Map.of("actionKey", "clickup:goal:create", "name", "Create Goal", "description", "Create a goal", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:goal:delete", "name", "Delete Goal", "description", "Delete a goal", "configSchema", List.of(Map.of("key", "goalId", "label", "Goal ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:goal:get", "name", "Get Goal", "description", "Get a goal", "configSchema", List.of(Map.of("key", "goalId", "label", "Goal ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:goal:getAll", "name", "Get All Goals", "description", "Get all goals", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:goal:update", "name", "Update Goal", "description", "Update a goal", "configSchema", List.of(Map.of("key", "goalId", "label", "Goal ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text"))),

                        // GOAL KEY RESULT
                        Map.of("actionKey", "clickup:goalKeyResult:create", "name", "Create Key Result", "description", "Create a key result", "configSchema", List.of(Map.of("key", "goalId", "label", "Goal ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:goalKeyResult:delete", "name", "Delete Key Result", "description", "Delete a key result", "configSchema", List.of(Map.of("key", "keyResultId", "label", "Key Result ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:goalKeyResult:update", "name", "Update Key Result", "description", "Update a key result", "configSchema", List.of(Map.of("key", "keyResultId", "label", "Key Result ID", "type", "text", "required", true))),

                        // GUEST
                        Map.of("actionKey", "clickup:guest:create", "name", "Invite Guest", "description", "Invite guest to workspace", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true), Map.of("key", "email", "label", "Email", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:guest:delete", "name", "Remove Guest", "description", "Remove guest from workspace", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true), Map.of("key", "guestId", "label", "Guest ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:guest:get", "name", "Get Guest", "description", "Get guest", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true), Map.of("key", "guestId", "label", "Guest ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:guest:update", "name", "Update Guest", "description", "Update guest", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true), Map.of("key", "guestId", "label", "Guest ID", "type", "text", "required", true))),

                        // COMMENT
                        Map.of("actionKey", "clickup:comment:create", "name", "Create Comment", "description", "Create a comment", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true), Map.of("key", "commentText", "label", "Comment Text", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:comment:delete", "name", "Delete Comment", "description", "Delete a comment", "configSchema", List.of(Map.of("key", "commentId", "label", "Comment ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:comment:getAll", "name", "Get All Comments", "description", "Get all comments", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:comment:update", "name", "Update Comment", "description", "Update a comment", "configSchema", List.of(Map.of("key", "commentId", "label", "Comment ID", "type", "text", "required", true), Map.of("key", "commentText", "label", "Comment Text", "type", "text", "required", true))),

                        // CHECKLIST
                        Map.of("actionKey", "clickup:checklist:create", "name", "Create Checklist", "description", "Create a checklist", "configSchema", List.of(Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:checklist:delete", "name", "Delete Checklist", "description", "Delete a checklist", "configSchema", List.of(Map.of("key", "checklistId", "label", "Checklist ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:checklist:update", "name", "Update Checklist", "description", "Update a checklist", "configSchema", List.of(Map.of("key", "checklistId", "label", "Checklist ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),

                        // CHECKLIST ITEM
                        Map.of("actionKey", "clickup:checklistItem:create", "name", "Create Item", "description", "Create checklist item", "configSchema", List.of(Map.of("key", "checklistId", "label", "Checklist ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:checklistItem:delete", "name", "Delete Item", "description", "Delete checklist item", "configSchema", List.of(Map.of("key", "checklistId", "label", "Checklist ID", "type", "text", "required", true), Map.of("key", "checklistItemId", "label", "Item ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:checklistItem:update", "name", "Update Item", "description", "Update checklist item", "configSchema", List.of(Map.of("key", "checklistId", "label", "Checklist ID", "type", "text", "required", true), Map.of("key", "checklistItemId", "label", "Item ID", "type", "text", "required", true))),

                        // SPACE TAG
                        Map.of("actionKey", "clickup:spaceTag:create", "name", "Create Tag", "description", "Create a space tag", "configSchema", List.of(Map.of("key", "spaceId", "label", "Space ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:spaceTag:delete", "name", "Delete Tag", "description", "Delete a space tag", "configSchema", List.of(Map.of("key", "spaceId", "label", "Space ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:spaceTag:getAll", "name", "Get All Tags", "description", "Get all space tags", "configSchema", List.of(Map.of("key", "spaceId", "label", "Space ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:spaceTag:update", "name", "Update Tag", "description", "Update a space tag", "configSchema", List.of(Map.of("key", "spaceId", "label", "Space ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),

                        // TIME ENTRY / TAG
                        Map.of("actionKey", "clickup:timeEntry:create", "name", "Create Time Entry", "description", "Create time entry", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true), Map.of("key", "taskId", "label", "Task ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:timeEntry:delete", "name", "Delete Time Entry", "description", "Delete time entry", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true), Map.of("key", "timeEntryId", "label", "Entry ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:timeEntry:get", "name", "Get Time Entry", "description", "Get time entry", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true), Map.of("key", "timeEntryId", "label", "Entry ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:timeEntry:getAll", "name", "Get All Time Entries", "description", "Get all time entries", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:timeEntry:update", "name", "Update Time Entry", "description", "Update time entry", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true), Map.of("key", "timeEntryId", "label", "Entry ID", "type", "text", "required", true))),

                        Map.of("actionKey", "clickup:timeEntryTag:getAll", "name", "Get Time Entry Tags", "description", "Get time entry tags", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true))),
                        Map.of("actionKey", "clickup:timeEntryTag:remove", "name", "Remove Time Entry Tag", "description", "Remove time entry tag", "configSchema", List.of(Map.of("key", "teamId", "label", "Team ID", "type", "text", "required", true), Map.of("key", "timeEntryId", "label", "Entry ID", "type", "text", "required", true), Map.of("key", "name", "label", "Tag Name", "type", "text", "required", true)))
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiToken", "label", "Personal API Token", "type", "password", "required", true)
        )).altAuthType(AuthType.OAUTH2).category("task-management");
    }
}
