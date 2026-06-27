package com.crescendo.apps.airtable;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Airtable Record handlers.
 */
@Component
public class AirtableRecordHandlers {

    @ActionMapping(appKey = "airtable", actionKey = "airtable:record:create")
    public Object createRecord(ActionContext context) throws Exception {
        String baseId = context.getString("baseId");
        String tableId = context.getString("tableId");
        Map<String, Object> fields = context.getMap("fields");

        Map<String, Object> body = new HashMap<>();
        body.put("records", List.of(Map.of("fields", fields)));

        return RestClient.builder()
                .url(AirtableSupport.getBaseUrl() + "/" + baseId + "/" + AirtableSupport.encode(tableId))
                .header("Authorization", AirtableSupport.getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "airtable", actionKey = "airtable:record:delete")
    public Object deleteRecord(ActionContext context) throws Exception {
        String baseId = context.getString("baseId");
        String tableId = context.getString("tableId");
        String recordId = context.getString("recordId");

        return RestClient.builder()
                .url(AirtableSupport.getBaseUrl() + "/" + baseId + "/" + AirtableSupport.encode(tableId) + "/" + recordId)
                .header("Authorization", AirtableSupport.getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "airtable", actionKey = "airtable:record:get")
    public Object getRecord(ActionContext context) throws Exception {
        String baseId = context.getString("baseId");
        String tableId = context.getString("tableId");
        String recordId = context.getString("recordId");

        return RestClient.builder()
                .url(AirtableSupport.getBaseUrl() + "/" + baseId + "/" + AirtableSupport.encode(tableId) + "/" + recordId)
                .header("Authorization", AirtableSupport.getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "airtable", actionKey = "airtable:record:search")
    public Object searchRecords(ActionContext context) throws Exception {
        String baseId = context.getString("baseId");
        String tableId = context.getString("tableId");
        String filterByFormula = context.getString("filterByFormula");
        Integer maxRecords = context.getInt("maxRecords");

        String url = AirtableSupport.getBaseUrl() + "/" + baseId + "/" + AirtableSupport.encode(tableId) + "?";
        if (filterByFormula != null && !filterByFormula.isBlank()) {
            url += "filterByFormula=" + AirtableSupport.encode(filterByFormula) + "&";
        }
        if (maxRecords != null) {
            url += "maxRecords=" + maxRecords;
        }

        return RestClient.builder()
                .url(url)
                .header("Authorization", AirtableSupport.getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "airtable", actionKey = "airtable:record:update")
    public Object updateRecord(ActionContext context) throws Exception {
        String baseId = context.getString("baseId");
        String tableId = context.getString("tableId");
        String recordId = context.getString("recordId");
        Map<String, Object> fields = context.getMap("fields");

        Map<String, Object> body = new HashMap<>();
        body.put("records", List.of(Map.of("id", recordId, "fields", fields)));

        return RestClient.builder()
                .url(AirtableSupport.getBaseUrl() + "/" + baseId + "/" + AirtableSupport.encode(tableId))
                .header("Authorization", AirtableSupport.getAuth(context))
                .header("Content-Type", "application/json")
                .patch(body)
                .execute();
    }

    @ActionMapping(appKey = "airtable", actionKey = "airtable:record:upsert")
    public Object upsertRecord(ActionContext context) throws Exception {
        String baseId = context.getString("baseId");
        String tableId = context.getString("tableId");
        Map<String, Object> fields = context.getMap("fields");
        @SuppressWarnings("unchecked")
        List<String> performUpsert = (List<String>) context.get("performUpsert");

        Map<String, Object> body = new HashMap<>();
        body.put("records", List.of(Map.of("fields", fields)));
        if (performUpsert != null && !performUpsert.isEmpty()) {
            body.put("performUpsert", Map.of("fieldsToMergeOn", performUpsert));
        }

        return RestClient.builder()
                .url(AirtableSupport.getBaseUrl() + "/" + baseId + "/" + AirtableSupport.encode(tableId))
                .header("Authorization", AirtableSupport.getAuth(context))
                .header("Content-Type", "application/json")
                .patch(body)
                .execute();
    }
}
