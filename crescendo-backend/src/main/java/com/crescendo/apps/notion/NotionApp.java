package com.crescendo.apps.notion;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Notion.
 *
 * Resources (from n8n Notion node):
 *   - block        : append, getAll
 *   - database     : get, getAll, search
 *   - databasePage : create, get, getAll, update
 *   - page         : archive, create, get, search, update
 *   - user         : get, getAll
 */
@Component
public class NotionApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "notion",
                "Notion",
                """
                Notion is an all-in-one workspace for your notes, tasks, wikis, and databases.
                
                This integration provides operations for:
                - **Block**: Append, Get All
                - **Database**: Get, Get All, Search
                - **Database Page**: Create, Get, Get All, Update
                - **Page**: Archive, Create, Get, Search, Update
                - **User**: Get, Get All
                
                Authenticate using a Notion Internal Integration Token or OAuth2.
                """,
                "https://www.google.com/s2/favicons?domain=notion.so&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // BLOCK
                        Map.of("actionKey", "notion:block:append", "name", "Append Block", "description", "Append a block to a parent", "configSchema", List.of(Map.of("key", "blockId", "label", "Block ID", "type", "text", "required", true), Map.of("key", "children", "label", "Children Blocks (JSON)", "type", "json", "required", true))),
                        Map.of("actionKey", "notion:block:getAll", "name", "Get All Blocks", "description", "Get all children blocks", "configSchema", List.of(Map.of("key", "blockId", "label", "Block ID", "type", "text", "required", true))),

                        // DATABASE
                        Map.of("actionKey", "notion:database:get", "name", "Get Database", "description", "Get a database", "configSchema", List.of(Map.of("key", "databaseId", "label", "Database ID", "type", "text", "required", true))),
                        Map.of("actionKey", "notion:database:getAll", "name", "Get All Databases", "description", "Get all databases", "configSchema", List.of()),
                        Map.of("actionKey", "notion:database:search", "name", "Search Database", "description", "Search for a database", "configSchema", List.of(Map.of("key", "query", "label", "Query", "type", "text"))),

                        // DATABASE PAGE
                        Map.of("actionKey", "notion:databasePage:create", "name", "Create Database Page", "description", "Create a page within a database", "configSchema", List.of(Map.of("key", "databaseId", "label", "Database ID", "type", "text", "required", true), Map.of("key", "properties", "label", "Properties (JSON)", "type", "json", "required", true))),
                        Map.of("actionKey", "notion:databasePage:get", "name", "Get Database Page", "description", "Get a page within a database", "configSchema", List.of(Map.of("key", "pageId", "label", "Page ID", "type", "text", "required", true))),
                        Map.of("actionKey", "notion:databasePage:getAll", "name", "Get All Database Pages", "description", "Get all pages within a database (query)", "configSchema", List.of(Map.of("key", "databaseId", "label", "Database ID", "type", "text", "required", true), Map.of("key", "filter", "label", "Filter (JSON)", "type", "json"))),
                        Map.of("actionKey", "notion:databasePage:update", "name", "Update Database Page", "description", "Update properties of a database page", "configSchema", List.of(Map.of("key", "pageId", "label", "Page ID", "type", "text", "required", true), Map.of("key", "properties", "label", "Properties (JSON)", "type", "json", "required", true))),

                        // PAGE
                        Map.of("actionKey", "notion:page:archive", "name", "Archive Page", "description", "Archive a page", "configSchema", List.of(Map.of("key", "pageId", "label", "Page ID", "type", "text", "required", true))),
                        Map.of("actionKey", "notion:page:create", "name", "Create Page", "description", "Create a page", "configSchema", List.of(Map.of("key", "pageId", "label", "Parent Page ID", "type", "text", "required", true), Map.of("key", "title", "label", "Title", "type", "text", "required", true))),
                        Map.of("actionKey", "notion:page:get", "name", "Get Page", "description", "Get a page", "configSchema", List.of(Map.of("key", "pageId", "label", "Page ID", "type", "text", "required", true))),
                        Map.of("actionKey", "notion:page:search", "name", "Search Page", "description", "Search for a page", "configSchema", List.of(Map.of("key", "query", "label", "Query", "type", "text"))),
                        Map.of("actionKey", "notion:page:update", "name", "Update Page", "description", "Update properties of a page", "configSchema", List.of(Map.of("key", "pageId", "label", "Page ID", "type", "text", "required", true), Map.of("key", "properties", "label", "Properties (JSON)", "type", "json", "required", true))),

                        // USER
                        Map.of("actionKey", "notion:user:get", "name", "Get User", "description", "Get a user", "configSchema", List.of(Map.of("key", "userId", "label", "User ID", "type", "text", "required", true))),
                        Map.of("actionKey", "notion:user:getAll", "name", "Get All Users", "description", "Get all users", "configSchema", List.of())
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiToken", "label", "Internal Integration Token", "type", "password", "required", true)
        )).altAuthType(AuthType.OAUTH2).category("task-management"); // Notion fits here or docs
    }
}
