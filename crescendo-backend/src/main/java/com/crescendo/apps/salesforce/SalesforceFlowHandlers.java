package com.crescendo.apps.salesforce;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SalesforceFlowHandlers {

    private String getBaseUrl(ActionContext context) {
        String instanceUrl = context.getCredential("instanceUrl");
        String apiVersion = context.getCredential("apiVersion");
        if (apiVersion == null || apiVersion.isBlank()) apiVersion = "v60.0";
        return instanceUrl + "/services/data/" + apiVersion;
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:flow:getAll")
    public Object getAllFlows(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(getBaseUrl(context) + "/actions/custom/flow")
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "salesforce", actionKey = "salesforce:flow:invoke")
    public Object invokeFlow(ActionContext context) throws Exception {
        Map<String, Object> payload = context.getMap("payload");
        String flowName = context.getString("flowName");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/actions/custom/flow/" + flowName)
                .header("Authorization", "Bearer " + context.getCredential("accessToken"))
                .header("Content-Type", "application/json")
                .post(payload)
                .execute();
    }
}
