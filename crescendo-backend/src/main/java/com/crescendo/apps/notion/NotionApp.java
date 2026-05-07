package com.crescendo.apps.notion;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NotionApp implements AppDefinition {

    @Override
    public App toApp() {
        var databaseField = Map.of("key", "databaseId", "label", "Database",
                "type", "dynamic_dropdown", "resourceType", "databases",
                "required", true,
                "helpText", "Select the Notion database");

        return new App(
            "notion", "Notion", "Create, update, and watch pages and databases in Notion",
            "/icons/notion.svg", AuthType.OAUTH2,

            // ═══ TRIGGERS ═══
            List.of(
                Map.of(
                    "triggerKey", "page-updated",
                    "name", "Page Updated",
                    "description", "Triggers when a Notion page is updated in a database",
                    "configSchema", List.of(databaseField)
                ),
                Map.of(
                    "triggerKey", "new-database-item",
                    "name", "New Database Item",
                    "description", "Triggers when a new page is added to a database",
                    "configSchema", List.of(databaseField)
                )
            ),

            // ═══ ACTIONS ═══
            List.of(
                Map.of(
                    "actionKey", "create-page",
                    "name", "Create Page",
                    "description", "Create a new page in a Notion database",
                    "configSchema", List.of(
                        databaseField,
                        Map.of("key", "title", "label", "Page Title",
                               "type", "text", "required", true,
                               "placeholder", "My New Page",
                               "helpText", "Title for the new page"),
                        Map.of("key", "status", "label", "Status",
                               "type", "select", "required", false,
                               "options", List.of(
                                   Map.of("value", "", "label", "None"),
                                   Map.of("value", "Not started", "label", "Not Started"),
                                   Map.of("value", "In progress", "label", "In Progress"),
                                   Map.of("value", "Done", "label", "Done")
                               ),
                               "helpText", "Page status (must match your database's Status options)"),
                        Map.of("key", "priority", "label", "Priority",
                               "type", "select", "required", false,
                               "options", List.of(
                                   Map.of("value", "", "label", "None"),
                                   Map.of("value", "High", "label", "High"),
                                   Map.of("value", "Medium", "label", "Medium"),
                                   Map.of("value", "Low", "label", "Low")
                               ),
                               "helpText", "Page priority (must match your database's Priority options)"),
                        Map.of("key", "dueDate", "label", "Due Date",
                               "type", "text", "required", false,
                               "placeholder", "2026-06-15",
                               "helpText", "Due date in YYYY-MM-DD format"),
                        Map.of("key", "assignee", "label", "Assignee",
                               "type", "text", "required", false,
                               "placeholder", "John Doe",
                               "helpText", "Name of the person to assign (must match a Person property)"),
                        Map.of("key", "tags", "label", "Tags",
                               "type", "text", "required", false,
                               "placeholder", "bug, frontend, urgent",
                               "helpText", "Comma-separated tags (must match your database's Multi-select options)"),
                        Map.of("key", "properties", "label", "Advanced: Extra Properties (JSON)",
                               "type", "json", "required", false,
                               "placeholder", "{\"Custom Field\": {\"rich_text\": [{\"text\": {\"content\": \"value\"}}]}}",
                               "helpText", "Optional raw JSON for additional Notion properties not covered above")
                    )
                ),
                Map.of(
                    "actionKey", "update-page",
                    "name", "Update Page",
                    "description", "Update properties of an existing Notion page",
                    "configSchema", List.of(
                        databaseField,
                        Map.<String, Object>of("key", "pageId", "label", "Page",
                               "type", "dynamic_dropdown", "resourceType", "pages",
                               "dependsOn", List.of("databaseId"),
                               "required", true,
                               "helpText", "Select the page to update"),
                        Map.of("key", "properties", "label", "Properties to Update",
                               "type", "json", "required", true,
                               "placeholder", "{\"Status\": {\"select\": {\"name\": \"Done\"}}}",
                               "helpText", "JSON object of Notion properties to update")
                    )
                ),
                Map.of(
                    "actionKey", "find-page",
                    "name", "Find Page",
                    "description", "Search for a page in a database by title or filter",
                    "configSchema", List.of(
                        databaseField,
                        Map.of("key", "query", "label", "Search Text",
                               "type", "text", "required", false,
                               "placeholder", "Project Alpha",
                               "helpText", "Search pages by title"),
                        Map.of("key", "maxResults", "label", "Max Results",
                               "type", "text", "required", false,
                               "placeholder", "10",
                               "helpText", "Maximum pages to return")
                    )
                ),
                Map.of(
                    "actionKey", "create-database",
                    "name", "Create Database",
                    "description", "Create a new inline database in a Notion page",
                    "configSchema", List.of(
                        Map.of("key", "parentPageId", "label", "Parent Page",
                               "type", "dynamic_dropdown", "resourceType", "pages",
                               "required", true,
                               "helpText", "Select the parent page for the new database"),
                        Map.of("key", "title", "label", "Database Title",
                               "type", "text", "required", true,
                               "placeholder", "Tasks",
                               "helpText", "Title for the new database"),
                        Map.of("key", "schema", "label", "Columns (JSON)",
                               "type", "json", "required", false,
                               "placeholder", "{\"Name\": {\"title\": {}}, \"Status\": {\"select\": {}}}",
                               "helpText", "JSON schema for database properties")
                    )
                )
            )
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("https://www.notion.so/my-integrations");
    }
}
