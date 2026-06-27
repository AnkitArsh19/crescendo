package com.crescendo.apps.homeassistant;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HomeAssistantServiceHandlers {

    @ActionMapping(appKey = "homeassistant", actionKey = "homeassistant:service:call")
    public Object callService(ActionContext context) throws Exception {
        String domain = context.getString("domain");
        String service = context.getString("service");
        
        Object serviceData = context.configuration().get("serviceData");
        
        return RestClient.builder()
                .url(HomeAssistantSupport.getBaseUrl(context) + "/api/services/" + domain + "/" + service)
                .header("Authorization", HomeAssistantSupport.getAuth(context))
                .header("Content-Type", "application/json")
                .post(serviceData != null ? serviceData : Map.of())
                .execute();
    }
}
