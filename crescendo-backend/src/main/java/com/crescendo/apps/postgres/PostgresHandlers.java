package com.crescendo.apps.postgres;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Postgres handlers.
 */
@Component
public class PostgresHandlers {

    @ActionMapping(appKey = "postgres", actionKey = "postgres:deleteTable")
    public Object deleteTable(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Delete executed");
    }

    @ActionMapping(appKey = "postgres", actionKey = "postgres:executeQuery")
    public Object executeQuery(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Query executed");
    }

    @ActionMapping(appKey = "postgres", actionKey = "postgres:insert")
    public Object insert(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Insert executed");
    }

    @ActionMapping(appKey = "postgres", actionKey = "postgres:upsert")
    public Object upsert(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Upsert executed");
    }

    @ActionMapping(appKey = "postgres", actionKey = "postgres:select")
    public Object select(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Select executed");
    }

    @ActionMapping(appKey = "postgres", actionKey = "postgres:update")
    public Object update(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Update executed");
    }
}
