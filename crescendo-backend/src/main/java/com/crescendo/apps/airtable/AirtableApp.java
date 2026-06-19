package com.crescendo.apps.airtable;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AirtableApp implements AppDefinition {

    @Override
    public App toApp() {
        var baseField = Map.of("key", "baseId", "label", "Base",
                "type", "dynamic_dropdown", "resourceType", "bases",
                "required", true,
                "helpText", "Select the Airtable base");

        var tableField = Map.<String, Object>of("key", "tableId", "label", "Table",
                "type", "dynamic_dropdown", "resourceType", "tables",
                "dependsOn", List.of("baseId"),
                "required", true,
                "helpText", "Select the table within the base");

        return new App("airtable", "Airtable", "Create, update, and manage records in Airtable bases",
                "https://www.google.com/s2/favicons?domain=airtable.com&sz=128", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-record",
                        "name", "New Record",
                        "description", "Triggers when a new record is added to a table",
                        "configSchema", List.of(baseField, tableField)
                    ),
                    Map.of(
                        "triggerKey", "updated-record",
                        "name", "Updated Record",
                        "description", "Triggers when an existing record is modified",
                        "configSchema", List.of(baseField, tableField)
                    ),
                    Map.of(
                        "triggerKey", "new-or-updated-record",
                        "name", "New or Updated Record",
                        "description", "Triggers on record creation or modification",
                        "configSchema", List.of(baseField, tableField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "create-record",
                        "name", "Create Record",
                        "description", "Add a new record to an Airtable table",
                        "configSchema", List.of(
                            baseField, tableField,
                            Map.of("key", "fields", "label", "Field Values (JSON)",
                                   "type", "json", "required", true,
                                   "placeholder", "{\"Name\": \"Alice\", \"Email\": \"alice@example.com\"}",
                                   "helpText", "JSON object of field names → values")
                        )
                    ),
                    Map.of(
                        "actionKey", "update-record",
                        "name", "Update Record",
                        "description", "Update an existing record's fields",
                        "configSchema", List.of(
                            baseField, tableField,
                            Map.of("key", "recordId", "label", "Record ID",
                                   "type", "text", "required", true,
                                   "placeholder", "rec123abc",
                                   "helpText", "The ID of the record to update (from a trigger)"),
                            Map.of("key", "fields", "label", "Fields to Update (JSON)",
                                   "type", "json", "required", true,
                                   "placeholder", "{\"Status\": \"Done\"}",
                                   "helpText", "JSON object of field names → new values")
                        )
                    ),
                    Map.of(
                        "actionKey", "find-record",
                        "name", "Find Record",
                        "description", "Search for records using a formula",
                        "configSchema", List.of(
                            baseField, tableField,
                            Map.of("key", "filterByFormula", "label", "Filter Formula",
                                   "type", "text", "required", false,
                                   "placeholder", "{Name} = 'Alice'",
                                   "helpText", "Airtable formula to filter records"),
                            Map.of("key", "maxRecords", "label", "Max Records",
                                   "type", "text", "required", false,
                                   "placeholder", "10",
                                   "helpText", "Maximum records to return")
                        )
                    ),
                    Map.of(
                        "actionKey", "delete-record",
                        "name", "Delete Record",
                        "description", "Delete a record from a table",
                        "configSchema", List.of(
                            baseField, tableField,
                            Map.of("key", "recordId", "label", "Record ID",
                                   "type", "text", "required", true,
                                   "placeholder", "rec123abc",
                                   "helpText", "The ID of the record to delete")
                        )
                    ),
                    Map.of(
                        "actionKey", "list-records",
                        "name", "List Records",
                        "description", "Retrieve records from an Airtable table",
                        "configSchema", List.of(baseField, tableField)
                    )
                )
        )
        .credentialSchema(List.of()).altAuthType(AuthType.APIKEY)
        .category("productivity")
        .helpUrl("https://airtable.com/create/tokens");
    }
}
