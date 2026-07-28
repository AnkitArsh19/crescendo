package com.crescendo.apps.mysql;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MySQL handlers.
 */
@Component
public class MySqlHandlers {

    @ActionMapping(appKey = "mysql", actionKey = "deleteTable")
    public Object deleteTable(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Delete executed");
    }

    @ActionMapping(appKey = "mysql", actionKey = "executeQuery")
    public Object executeQuery(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Query executed");
    }

    @ActionMapping(appKey = "mysql", actionKey = "insert")
    public Object insert(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Insert executed");
    }

    @ActionMapping(appKey = "mysql", actionKey = "upsert")
    public Object upsert(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Upsert executed");
    }

    @ActionMapping(appKey = "mysql", actionKey = "select")
    public Object select(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Select executed");
    }

    @ActionMapping(appKey = "mysql", actionKey = "update")
    public Object update(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Update executed");
    }
}
