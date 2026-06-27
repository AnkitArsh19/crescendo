package com.crescendo.apps.typeform;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

@Component
public class TypeformHandlers {

    private String getBaseUrl() {
        return "https://api.typeform.com";
    }

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "typeform", actionKey = "typeform:form:getAll")
    public Object getAllForms(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(getBaseUrl() + "/forms")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "typeform", actionKey = "typeform:response:getAll")
    public Object getResponses(ActionContext context) throws Exception {
        String formId = context.getString("formId");
        return RestClient.builder()
                .url(getBaseUrl() + "/forms/" + formId + "/responses")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }
}
