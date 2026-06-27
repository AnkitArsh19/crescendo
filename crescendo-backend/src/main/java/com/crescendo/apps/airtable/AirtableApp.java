package com.crescendo.apps.airtable;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Airtable.
 *
 * Resources (from n8n Airtable v2 node):
 *   - base   : getMany, getSchema
 *   - record : create, delete, get, search, update, upsert
 */
@Component
public class AirtableApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "airtable",
                "Airtable",
                """
                Airtable is a cloud collaboration service that provides the features of a database applied to a spreadsheet.
                
                This integration provides operations for:
                - **Base**: Get Many, Get Schema
                - **Record**: Create, Delete, Get, Search, Update, Upsert
                
                Authenticate using an Airtable Personal Access Token (PAT) or OAuth2.
                """,
                "https://www.google.com/s2/favicons?domain=airtable.com&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // BASE
                        Map.of("actionKey", "airtable:base:getMany", "name", "Get Many Bases", "description", "List bases", "configSchema", List.of()),
                        Map.of("actionKey", "airtable:base:getSchema", "name", "Get Schema", "description", "Get base schema", "configSchema", List.of(Map.of("key", "baseId", "label", "Base ID", "type", "text", "required", true))),

                        // RECORD
                        Map.of("actionKey", "airtable:record:create", "name", "Create Record", "description", "Create a record", "configSchema", List.of(Map.of("key", "baseId", "label", "Base ID", "type", "text", "required", true), Map.of("key", "tableId", "label", "Table ID or Name", "type", "text", "required", true), Map.of("key", "fields", "label", "Fields (JSON)", "type", "json", "required", true))),
                        Map.of("actionKey", "airtable:record:delete", "name", "Delete Record", "description", "Delete a record", "configSchema", List.of(Map.of("key", "baseId", "label", "Base ID", "type", "text", "required", true), Map.of("key", "tableId", "label", "Table ID or Name", "type", "text", "required", true), Map.of("key", "recordId", "label", "Record ID", "type", "text", "required", true))),
                        Map.of("actionKey", "airtable:record:get", "name", "Get Record", "description", "Get a record", "configSchema", List.of(Map.of("key", "baseId", "label", "Base ID", "type", "text", "required", true), Map.of("key", "tableId", "label", "Table ID or Name", "type", "text", "required", true), Map.of("key", "recordId", "label", "Record ID", "type", "text", "required", true))),
                        Map.of("actionKey", "airtable:record:search", "name", "Search Records", "description", "Search for records", "configSchema", List.of(Map.of("key", "baseId", "label", "Base ID", "type", "text", "required", true), Map.of("key", "tableId", "label", "Table ID or Name", "type", "text", "required", true), Map.of("key", "filterByFormula", "label", "Filter Formula", "type", "text"), Map.of("key", "maxRecords", "label", "Max Records", "type", "number"))),
                        Map.of("actionKey", "airtable:record:update", "name", "Update Record", "description", "Update a record", "configSchema", List.of(Map.of("key", "baseId", "label", "Base ID", "type", "text", "required", true), Map.of("key", "tableId", "label", "Table ID or Name", "type", "text", "required", true), Map.of("key", "recordId", "label", "Record ID", "type", "text", "required", true), Map.of("key", "fields", "label", "Fields (JSON)", "type", "json", "required", true))),
                        Map.of("actionKey", "airtable:record:upsert", "name", "Upsert Record", "description", "Create or update a record", "configSchema", List.of(Map.of("key", "baseId", "label", "Base ID", "type", "text", "required", true), Map.of("key", "tableId", "label", "Table ID or Name", "type", "text", "required", true), Map.of("key", "fields", "label", "Fields (JSON)", "type", "json", "required", true), Map.of("key", "performUpsert", "label", "Perform Upsert (Fields to merge on)", "type", "json")))
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiToken", "label", "Personal Access Token", "type", "password", "required", true)
        )).altAuthType(AuthType.OAUTH2).category("docs-workspace");
    }
}
