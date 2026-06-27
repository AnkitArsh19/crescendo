package com.crescendo.apps.jenkins;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

/**
 * Jenkins handlers.
 */
@Component
public class JenkinsHandlers {

    private String getBaseUrl(ActionContext context) {
        String server = context.getCredential("server");
        if (server != null && server.endsWith("/")) {
            server = server.substring(0, server.length() - 1);
        }
        return server;
    }

    private String getAuth(ActionContext context) {
        String username = context.getCredential("username");
        String password = context.getCredential("password");
        if (username != null && password != null) {
            return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        }
        return null;
    }

    @ActionMapping(appKey = "jenkins", actionKey = "jenkins:build:trigger")
    public Object triggerBuild(ActionContext context) throws Exception {
        String jobName = context.getString("jobName");
        Map<String, Object> parameters = context.getMap("parameters");
        
        String url = getBaseUrl(context) + "/job/" + jobName;
        if (parameters != null && !parameters.isEmpty()) {
            url += "/buildWithParameters";
        } else {
            url += "/build";
        }

        RestClient.Builder builder = RestClient.builder().url(url);
        String auth = getAuth(context);
        if (auth != null) builder.header("Authorization", auth);

        return builder.post(parameters != null ? parameters : Map.of()).execute();
    }

    @ActionMapping(appKey = "jenkins", actionKey = "jenkins:build:get")
    public Object getBuild(ActionContext context) throws Exception {
        String jobName = context.getString("jobName");
        String buildNumber = context.getString("buildNumber");

        RestClient.Builder builder = RestClient.builder()
                .url(getBaseUrl(context) + "/job/" + jobName + "/" + buildNumber + "/api/json");
        String auth = getAuth(context);
        if (auth != null) builder.header("Authorization", auth);

        return builder.get().execute();
    }

    @ActionMapping(appKey = "jenkins", actionKey = "jenkins:job:copy")
    public Object copyJob(ActionContext context) throws Exception {
        String name = context.getString("name");
        String from = context.getString("from");

        RestClient.Builder builder = RestClient.builder()
                .url(getBaseUrl(context) + "/createItem?name=" + name + "&mode=copy&from=" + from);
        String auth = getAuth(context);
        if (auth != null) builder.header("Authorization", auth);

        return builder.post(Map.of()).execute();
    }

    @ActionMapping(appKey = "jenkins", actionKey = "jenkins:instance:getConfig")
    public Object getInstanceConfig(ActionContext context) throws Exception {
        RestClient.Builder builder = RestClient.builder()
                .url(getBaseUrl(context) + "/api/json");
        String auth = getAuth(context);
        if (auth != null) builder.header("Authorization", auth);

        return builder.get().execute();
    }
}
