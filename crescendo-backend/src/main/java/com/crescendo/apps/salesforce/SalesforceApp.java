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
                "Salesforce", """
Salesforce is the world's leading cloud-based CRM software. It provides customer relationship management service and also provides a complementary suite of enterprise applications focused on customer service, marketing automation, analytics, and application development.
                """,
                "https://www.google.com/s2/favicons?domain=salesforce.com&sz=128",
                AuthType.OAUTH2,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "salesforce:account:addNote",
                                "name", "Addnote Account",
                                "description", "Addnote a Account in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:account:create",
                                "name", "Create Account",
                                "description", "Create a Account in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:account:upsert",
                                "name", "Upsert Account",
                                "description", "Upsert a Account in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:account:delete",
                                "name", "Delete Account",
                                "description", "Delete a Account in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:account:get",
                                "name", "Get Account",
                                "description", "Get a Account in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:account:getAll",
                                "name", "Getall Account",
                                "description", "Getall a Account in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:account:getSummary",
                                "name", "Getsummary Account",
                                "description", "Getsummary a Account in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:account:update",
                                "name", "Update Account",
                                "description", "Update a Account in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:attachment:create",
                                "name", "Create Attachment",
                                "description", "Create a Attachment in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:attachment:delete",
                                "name", "Delete Attachment",
                                "description", "Delete a Attachment in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:attachment:get",
                                "name", "Get Attachment",
                                "description", "Get a Attachment in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:attachment:getAll",
                                "name", "Getall Attachment",
                                "description", "Getall a Attachment in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:attachment:getSummary",
                                "name", "Getsummary Attachment",
                                "description", "Getsummary a Attachment in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:attachment:update",
                                "name", "Update Attachment",
                                "description", "Update a Attachment in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:case:addComment",
                                "name", "Addcomment Case",
                                "description", "Addcomment a Case in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:case:create",
                                "name", "Create Case",
                                "description", "Create a Case in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:case:delete",
                                "name", "Delete Case",
                                "description", "Delete a Case in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:case:get",
                                "name", "Get Case",
                                "description", "Get a Case in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:case:getAll",
                                "name", "Getall Case",
                                "description", "Getall a Case in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:case:getSummary",
                                "name", "Getsummary Case",
                                "description", "Getsummary a Case in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:case:update",
                                "name", "Update Case",
                                "description", "Update a Case in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:contact:addToCampaign",
                                "name", "Addtocampaign Contact",
                                "description", "Addtocampaign a Contact in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:contact:addNote",
                                "name", "Addnote Contact",
                                "description", "Addnote a Contact in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:contact:create",
                                "name", "Create Contact",
                                "description", "Create a Contact in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:contact:upsert",
                                "name", "Upsert Contact",
                                "description", "Upsert a Contact in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:contact:delete",
                                "name", "Delete Contact",
                                "description", "Delete a Contact in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:contact:get",
                                "name", "Get Contact",
                                "description", "Get a Contact in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:contact:getAll",
                                "name", "Getall Contact",
                                "description", "Getall a Contact in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:contact:getSummary",
                                "name", "Getsummary Contact",
                                "description", "Getsummary a Contact in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:contact:update",
                                "name", "Update Contact",
                                "description", "Update a Contact in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:customObject:create",
                                "name", "Create CustomObject",
                                "description", "Create a CustomObject in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:customObject:upsert",
                                "name", "Upsert CustomObject",
                                "description", "Upsert a CustomObject in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:customObject:delete",
                                "name", "Delete CustomObject",
                                "description", "Delete a CustomObject in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:customObject:get",
                                "name", "Get CustomObject",
                                "description", "Get a CustomObject in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:customObject:getAll",
                                "name", "Getall CustomObject",
                                "description", "Getall a CustomObject in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:customObject:update",
                                "name", "Update CustomObject",
                                "description", "Update a CustomObject in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:document:upload",
                                "name", "Upload Document",
                                "description", "Upload a Document in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:flow:getAll",
                                "name", "Getall Flow",
                                "description", "Getall a Flow in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:flow:invoke",
                                "name", "Invoke Flow",
                                "description", "Invoke a Flow in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:lead:addToCampaign",
                                "name", "Addtocampaign Lead",
                                "description", "Addtocampaign a Lead in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:lead:addNote",
                                "name", "Addnote Lead",
                                "description", "Addnote a Lead in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:lead:create",
                                "name", "Create Lead",
                                "description", "Create a Lead in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:lead:upsert",
                                "name", "Upsert Lead",
                                "description", "Upsert a Lead in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:lead:delete",
                                "name", "Delete Lead",
                                "description", "Delete a Lead in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:lead:get",
                                "name", "Get Lead",
                                "description", "Get a Lead in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:lead:getAll",
                                "name", "Getall Lead",
                                "description", "Getall a Lead in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:lead:getSummary",
                                "name", "Getsummary Lead",
                                "description", "Getsummary a Lead in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:lead:update",
                                "name", "Update Lead",
                                "description", "Update a Lead in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:opportunity:addNote",
                                "name", "Addnote Opportunity",
                                "description", "Addnote a Opportunity in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:opportunity:create",
                                "name", "Create Opportunity",
                                "description", "Create a Opportunity in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:opportunity:upsert",
                                "name", "Upsert Opportunity",
                                "description", "Upsert a Opportunity in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:opportunity:delete",
                                "name", "Delete Opportunity",
                                "description", "Delete a Opportunity in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:opportunity:get",
                                "name", "Get Opportunity",
                                "description", "Get a Opportunity in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:opportunity:getAll",
                                "name", "Getall Opportunity",
                                "description", "Getall a Opportunity in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:opportunity:getSummary",
                                "name", "Getsummary Opportunity",
                                "description", "Getsummary a Opportunity in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:opportunity:update",
                                "name", "Update Opportunity",
                                "description", "Update a Opportunity in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:search:query",
                                "name", "Query Search",
                                "description", "Query a Search in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:task:create",
                                "name", "Create Task",
                                "description", "Create a Task in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:task:delete",
                                "name", "Delete Task",
                                "description", "Delete a Task in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:task:get",
                                "name", "Get Task",
                                "description", "Get a Task in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:task:getAll",
                                "name", "Getall Task",
                                "description", "Getall a Task in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:task:getSummary",
                                "name", "Getsummary Task",
                                "description", "Getsummary a Task in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:task:update",
                                "name", "Update Task",
                                "description", "Update a Task in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:user:get",
                                "name", "Get User",
                                "description", "Get a User in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "salesforce:user:getAll",
                                "name", "Getall User",
                                "description", "Getall a User in Salesforce",
                                "configSchema", List.of(
                                        Map.of("key", "payload", "label", "JSON Payload", "type", "json", "required", false)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "instanceUrl", "label", "Instance URL", "type", "text", "required", true, "placeholder", "https://your-domain.my.salesforce.com"),
                Map.of("key", "accessToken", "label", "Access Token", "type", "password", "required", true),
                Map.of("key", "apiVersion", "label", "API Version", "type", "text", "required", false, "placeholder", "v60.0")
        )).altAuthType(AuthType.OAUTH2).category("crm");
    }
}
