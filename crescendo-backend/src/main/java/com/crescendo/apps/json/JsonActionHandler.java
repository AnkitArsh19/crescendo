package com.crescendo.apps.json;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Component
@ActionMapping(appKey = "json", actionKey = "parse")
public class JsonActionHandler implements ActionHandler {

    private final ObjectMapper mapper;

    public JsonActionHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String actionKey = context.actionKey();
        
        String propertyName = config.getOrDefault("propertyName", "data").toString();
        String destinationProperty = config.containsKey("destinationProperty") && config.get("destinationProperty") != null && !config.get("destinationProperty").toString().isBlank() ? 
            config.get("destinationProperty").toString() : propertyName;
            
        Map<String, Object> input = context.input() != null ? new HashMap<>(context.input()) : new HashMap<>();
        
        if (!input.containsKey(propertyName)) {
            return ActionResult.failure("Input does not contain property: " + propertyName);
        }
        
        try {
            if ("parse".equals(actionKey)) {
                String str = String.valueOf(input.get(propertyName));
                Object parsed = mapper.readValue(str, Object.class);
                input.put(destinationProperty, parsed);
                return ActionResult.success(input);
            } else if ("stringify".equals(actionKey)) {
                Object obj = input.get(propertyName);
                String str = mapper.writeValueAsString(obj);
                input.put(destinationProperty, str);
                return ActionResult.success(input);
            } else {
                return ActionResult.failure("Unknown action: " + actionKey);
            }
        } catch (Exception e) {
            return ActionResult.failure("JSON operation failed: " + e.getMessage());
        }
    }
}
