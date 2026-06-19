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

        return new App("google-tasks", "Google Tasks", "Create, complete, and manage Google Tasks",
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
                        "actionKey", "create-task",
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
                                   "helpText", "Due datetime in ISO 8601 format")
                        )
                    ),
                    Map.of(
                        "actionKey", "complete-task",
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
                        "actionKey", "delete-task",
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
                        "actionKey", "list-tasks",
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
                                   "helpText", "Maximum tasks to return")
                        )
                    ),
                    Map.of(
                        "actionKey", "update-task",
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
                                   "helpText", "New status")
                        )
                    )
                ))
                .credentialSchema(List.of())
                .category("productivity")
                .helpUrl("https://ssl.gstatic.com/images/branding/product/2x/tasks_48dp.png");
    }
}