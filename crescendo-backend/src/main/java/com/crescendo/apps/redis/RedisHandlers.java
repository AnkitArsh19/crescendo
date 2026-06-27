package com.crescendo.apps.redis;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis handlers.
 */
@Component
public class RedisHandlers {

    @ActionMapping(appKey = "redis", actionKey = "redis:delete")
    public Object delete(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Delete executed");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:get")
    public Object get(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Get executed");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:incr")
    public Object incr(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Increment executed");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:info")
    public Object info(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Info executed");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:keys")
    public Object keys(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Keys executed");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:llen")
    public Object llen(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "List length executed");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:pop")
    public Object pop(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Pop executed");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:publish")
    public Object publish(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Publish executed");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:push")
    public Object push(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Push executed");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:set")
    public Object set(ActionContext context) throws Exception {
        return Map.of("status", "success", "message", "Set executed");
    }
}
