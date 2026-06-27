package com.crescendo.apps.airtable;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

/**
 * Airtable Base handlers.
 */
@Component
public class AirtableBaseHandlers {

    @ActionMapping(appKey = "airtable", actionKey = "airtable:base:getMany")
    public Object getManyBases(ActionContext context) throws Exception {
        return RestClient.builder()
                .url("https://api.airtable.com/v0/meta/bases")
                .header("Authorization", AirtableSupport.getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "airtable", actionKey = "airtable:base:getSchema")
    public Object getBaseSchema(ActionContext context) throws Exception {
        String baseId = context.getString("baseId");
        return RestClient.builder()
                .url("https://api.airtable.com/v0/meta/bases/" + baseId + "/tables")
                .header("Authorization", AirtableSupport.getAuth(context))
                .get()
                .execute();
    }
}
