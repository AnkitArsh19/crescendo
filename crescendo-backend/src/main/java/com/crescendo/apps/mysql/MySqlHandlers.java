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

    @ActionMapping(appKey = "mySql", actionKey = "mySql:deleteTable")
    public Object deleteTable(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Delete executed");
    }

    @ActionMapping(appKey = "mySql", actionKey = "mySql:executeQuery")
    public Object executeQuery(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Query executed");
    }

    @ActionMapping(appKey = "mySql", actionKey = "mySql:insert")
    public Object insert(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Insert executed");
    }

    @ActionMapping(appKey = "mySql", actionKey = "mySql:upsert")
    public Object upsert(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Upsert executed");
    }

    @ActionMapping(appKey = "mySql", actionKey = "mySql:select")
    public Object select(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Select executed");
    }

    @ActionMapping(appKey = "mySql", actionKey = "mySql:update")
    public Object update(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Update executed");
    }
}
