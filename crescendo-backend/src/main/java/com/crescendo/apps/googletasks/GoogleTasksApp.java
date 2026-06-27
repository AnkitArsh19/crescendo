package com.crescendo.apps.googletasks;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GoogleTasksApp implements AppDefinition {

    @Override
    public App toApp() {
        var taskListField = Map.of("key", "taskListId", "label", "Task List",
                "type", "dynamic_dropdown", "resourceType", "taskLists",
                "required", true,
                "helpText", "Select which task list to use");

        return new App("google-tasks", "Google Tasks", """
                Google Tasks is a task management application developed by Google. The Crescendo Google Tasks app allows you to manage your to-do lists seamlessly from other tools.

                **What you can do with Google Tasks in Crescendo:**
                - Add a new task to your default list when a Slack message is saved
                - Automatically complete a Google Task when a corresponding GitHub issue is closed
                - Create a task with a due date assigned from an incoming email
                - Fetch pending tasks to generate a daily digest in Telegram

                **Actions available:**
                - Create Task — add a new item to a specific task list
                - Complete Task — mark an existing task as finished
                - List Tasks — retrieve pending items

                **Who should use this:** Individuals looking to unify their personal to-dos, professionals managing their daily priorities, and assistants organizing tasks.

                **Authentication:** OAuth 2.0 (connect your Google account).
                """,
                "https://upload.wikimedia.org/wikipedia/commons/5/5b/Google_Tasks_2021.svg", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "task-completed",
                        "name", "Task Completed",
                        "description", "Triggers when a task is marked completed",
                        "configSchema", List.of(taskListField)
                    ),
                    Map.of(
                        "triggerKey", "new-task",
                        "name", "New Task Created",
                        "description", "Triggers when a new task is created",
                        "configSchema", List.of(taskListField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "create",
                        "name", "Create Task",
                        "description", "Create a task in a Google Tasks list",
                        "configSchema", List.of(
                            taskListField,
                            Map.of("key", "title", "label", "Task Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Review pull request",
                                   "helpText", "Title of the task"),
                            Map.of("key", "notes", "label", "Notes",
                                   "type", "textarea", "required", false,
                                   "helpText", "Optional task notes"),
                            Map.of("key", "due", "label", "Due Date",
                                   "type", "text", "required", false,
                                   "placeholder", "2025-03-20T00:00:00Z",
                                   "helpText", "Due datetime in ISO 8601 format"),
                            Map.of("key", "completed", "label", "Completion Date",
                                   "type", "text", "required", false,
                                   "helpText", "Completion date of the task (RFC 3339)"),
                            Map.of("key", "deleted", "label", "Deleted",
                                   "type", "select", "required", false,
                                   "options", List.of(Map.of("label", "True", "value", "true"), Map.of("label", "False", "value", "false"))),
                            Map.of("key", "status", "label", "Status",
                                   "type", "select", "required", false,
                                   "options", List.of(Map.of("label", "Needs Action", "value", "needsAction"), Map.of("label", "Completed", "value", "completed"))),
                            Map.of("key", "parent", "label", "Parent Task ID",
                                   "type", "text", "required", false),
                            Map.of("key", "previous", "label", "Previous Task ID",
                                   "type", "text", "required", false)
                        )
                    ),
                    Map.of(
                        "actionKey", "complete",
                        "name", "Complete Task",
                        "description", "Mark a task as completed",
                        "configSchema", List.of(
                            taskListField,
                            Map.<String, Object>of("key", "taskId", "label", "Task",
                                   "type", "dynamic_dropdown", "resourceType", "tasks",
                                   "dependsOn", List.of("taskListId"),
                                   "required", true,
                                   "helpText", "Select the task to complete")
                        )
                    ),
                    Map.of(
                        "actionKey", "delete",
                        "name", "Delete Task",
                        "description", "Delete a task from a list",
                        "configSchema", List.of(
                            taskListField,
                            Map.<String, Object>of("key", "taskId", "label", "Task",
                                   "type", "dynamic_dropdown", "resourceType", "tasks",
                                   "dependsOn", List.of("taskListId"),
                                   "required", true,
                                   "helpText", "Select the task to delete")
                        )
                    ),
                    Map.of(
                        "actionKey", "getAll",
                        "name", "List Tasks",
                        "description", "List tasks from a Google Tasks list",
                        "configSchema", List.of(
                            taskListField,
                            Map.of("key", "showCompleted", "label", "Show Completed",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(
                                       Map.of("value", "true", "label", "Yes"),
                                       Map.of("value", "false", "label", "No")
                                   ),
                                   "helpText", "Include completed tasks"),
                            Map.of("key", "maxResults", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "20",
                                   "helpText", "Maximum tasks to return"),
                            Map.of("key", "completedMax", "label", "Completed Max",
                                   "type", "text", "required", false,
                                   "helpText", "Upper bound for completion date (RFC 3339)"),
                            Map.of("key", "completedMin", "label", "Completed Min",
                                   "type", "text", "required", false,
                                   "helpText", "Lower bound for completion date (RFC 3339)"),
                            Map.of("key", "dueMax", "label", "Due Max",
                                   "type", "text", "required", false,
                                   "helpText", "Upper bound for due date (RFC 3339)"),
                            Map.of("key", "dueMin", "label", "Due Min",
                                   "type", "text", "required", false,
                                   "helpText", "Lower bound for due date (RFC 3339)"),
                            Map.of("key", "showDeleted", "label", "Show Deleted",
                                   "type", "select", "required", false,
                                   "options", List.of(Map.of("label", "True", "value", "true"), Map.of("label", "False", "value", "false"))),
                            Map.of("key", "showHidden", "label", "Show Hidden",
                                   "type", "select", "required", false,
                                   "options", List.of(Map.of("label", "True", "value", "true"), Map.of("label", "False", "value", "false"))),
                            Map.of("key", "updatedMin", "label", "Updated Min",
                                   "type", "text", "required", false,
                                   "helpText", "Lower bound for modification time (RFC 3339)")
                        )
                    ),
                    Map.of(
                        "actionKey", "update",
                        "name", "Update Task",
                        "description", "Modify a task's title, notes, due date, or status",
                        "configSchema", List.of(
                            taskListField,
                            Map.<String, Object>of("key", "taskId", "label", "Task",
                                   "type", "dynamic_dropdown", "resourceType", "tasks",
                                   "dependsOn", List.of("taskListId"),
                                   "required", true,
                                   "helpText", "Select the task to update"),
                            Map.of("key", "title", "label", "Title",
                                   "type", "text", "required", false,
                                   "helpText", "Updated task title"),
                            Map.of("key", "notes", "label", "Notes",
                                   "type", "textarea", "required", false,
                                   "helpText", "Updated task notes"),
                            Map.of("key", "status", "label", "Status",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "needsAction", "label", "Needs Action"),
                                       Map.of("value", "completed", "label", "Completed")
                                   ),
                                   "helpText", "New status"),
                            Map.of("key", "due", "label", "Due Date",
                                   "type", "text", "required", false,
                                   "helpText", "Updated due datetime (RFC 3339)"),
                            Map.of("key", "completed", "label", "Completion Date",
                                   "type", "text", "required", false,
                                   "helpText", "Updated completion date (RFC 3339)"),
                            Map.of("key", "deleted", "label", "Deleted",
                                   "type", "select", "required", false,
                                   "options", List.of(Map.of("label", "True", "value", "true"), Map.of("label", "False", "value", "false"))),
                            Map.of("key", "previous", "label", "Previous Task ID",
                                   "type", "text", "required", false)
                        )
                    )
                ))
                .credentialSchema(List.of())
                .category("productivity")
                .helpUrl("https://ssl.gstatic.com/images/branding/product/2x/tasks_48dp.png");
    }
}