package com.crescendo.apps.salesforce;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SalesforceApp implements AppDefinition {
    public App toApp() {
        return new App(
                "salesforce",
                "Salesforce",
                "Query and create Salesforce records",
                "/icons/salesforce.svg",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "query",
                                "name", "SOQL Query",
                                "description", "Run a SOQL query",
                                "configSchema", List.of(
                                        Map.of("key", "soql", "label", "SOQL", "type", "textarea", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "create-record",
                                "name", "Create Record",
                                "description", "Create an sObject record",
                                "configSchema", List.of(
                                        Map.of("key", "object", "label", "Object", "type", "text", "required", true, "placeholder", "Contact"),
                                        Map.of("key", "fields", "label", "Fields (JSON)", "type", "json", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "instanceUrl", "label", "Instance URL", "type", "text", "required", true, "placeholder", "https://your-domain.my.salesforce.com"),
                Map.of("key", "accessToken", "label", "Access Token", "type", "password", "required", true),
                Map.of("key", "apiVersion", "label", "API Version", "type", "text", "required", false, "placeholder", "v60.0")
        )).altAuthType(AuthType.OAUTH2).category("crm").helpUrl("https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/");
    }
}
