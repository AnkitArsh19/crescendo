package com.crescendo.apps.homeassistant;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

// import java.util.Map;

@Component
public class HomeAssistantStateHandlers {

    @ActionMapping(appKey = "homeassistant", actionKey = "homeassistant:state:get")
    public Object getState(ActionContext context) throws Exception {
        String entityId = context.getString("entityId");
        
        return RestClient.builder()
                .url(HomeAssistantSupport.getBaseUrl(context) + "/api/states/" + entityId)
                .header("Authorization", HomeAssistantSupport.getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "homeassistant", actionKey = "homeassistant:state:getMany")
    public Object getStates(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(HomeAssistantSupport.getBaseUrl(context) + "/api/states")
                .header("Authorization", HomeAssistantSupport.getAuth(context))
                .get()
                .execute();
    }
}
