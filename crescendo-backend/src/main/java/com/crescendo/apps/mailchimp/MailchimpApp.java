package com.crescendo.apps.mailchimp;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Mailchimp.
 *
 * Resources (from n8n Mailchimp.node.ts):
 *   - campaign    : create, delete, get, getAll, replicate, resend, send
 *   - listGroup   : create, delete, getAll
 *   - member      : createOrUpdate, delete, get, getAll, update, tag (addTags)
 *   - memberTag   : add, remove
 */
@Component
public class MailchimpApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "mailchimp",
                "Mailchimp",
                """
                Mailchimp is an email marketing service provider. It allows you to build audiences, design campaigns, and automate your email marketing.
                
                This integration mirrors n8n's Mailchimp node and provides:
                - **Campaign**: Create, Delete, Get, Get All, Replicate, Resend, Send
                - **List Group (Interest)**: Create, Delete, Get All
                - **Member**: Create or Update, Delete, Get, Get All, Update, Tag
                - **Member Tag**: Add Tags, Remove Tags
                
                Authenticate with an API Key (generated in Mailchimp account settings) or OAuth2.
                """,
                "https://www.google.com/s2/favicons?domain=mailchimp.com&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // CAMPAIGN
                        Map.of("actionKey", "mailchimp:campaign:create", "name", "Create Campaign", "description", "Create a campaign", "configSchema", List.of(
                                Map.of("key", "type", "label", "Type (regular, plaintext, absplit, rss, variate)", "type", "text", "required", true),
                                Map.of("key", "listId", "label", "List / Audience ID", "type", "text"),
                                Map.of("key", "subject", "label", "Subject Line", "type", "text"),
                                Map.of("key", "fromName", "label", "From Name", "type", "text"),
                                Map.of("key", "replyTo", "label", "Reply-to Email", "type", "text"),
                                Map.of("key", "settings", "label", "Additional Settings (JSON)", "type", "json")
                        )),
                        Map.of("actionKey", "mailchimp:campaign:delete", "name", "Delete Campaign", "description", "Delete a campaign", "configSchema", List.of(
                                Map.of("key", "campaignId", "label", "Campaign ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "mailchimp:campaign:get", "name", "Get Campaign", "description", "Get a campaign", "configSchema", List.of(
                                Map.of("key", "campaignId", "label", "Campaign ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "mailchimp:campaign:getAll", "name", "Get All Campaigns", "description", "Get all campaigns", "configSchema", List.of(
                                Map.of("key", "status", "label", "Status Filter (save, sending, sent, schedule)", "type", "text"),
                                Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100)
                        )),
                        Map.of("actionKey", "mailchimp:campaign:replicate", "name", "Replicate Campaign", "description", "Replicate a campaign in saved or send status", "configSchema", List.of(
                                Map.of("key", "campaignId", "label", "Campaign ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "mailchimp:campaign:resend", "name", "Resend Campaign", "description", "Creates a Resend to Non-Openers version of this campaign", "configSchema", List.of(
                                Map.of("key", "campaignId", "label", "Campaign ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "mailchimp:campaign:send", "name", "Send Campaign", "description", "Send a Mailchimp campaign", "configSchema", List.of(
                                Map.of("key", "campaignId", "label", "Campaign ID", "type", "text", "required", true)
                        )),

                        // LIST GROUP (Interest Category)
                        Map.of("actionKey", "mailchimp:listGroup:create", "name", "Create List Group", "description", "Create a list group for a list", "configSchema", List.of(
                                Map.of("key", "listId", "label", "List / Audience ID", "type", "text", "required", true),
                                Map.of("key", "name", "label", "Group Name", "type", "text", "required", true),
                                Map.of("key", "type", "label", "Type (checkboxes, dropdown, radio, hidden)", "type", "text", "required", true),
                                Map.of("key", "title", "label", "Title", "type", "text")
                        )),
                        Map.of("actionKey", "mailchimp:listGroup:delete", "name", "Delete List Group", "description", "Delete a list group", "configSchema", List.of(
                                Map.of("key", "listId", "label", "List / Audience ID", "type", "text", "required", true),
                                Map.of("key", "groupId", "label", "Group ID", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "mailchimp:listGroup:getAll", "name", "Get All List Groups", "description", "Get all groups for a list", "configSchema", List.of(
                                Map.of("key", "listId", "label", "List / Audience ID", "type", "text", "required", true)
                        )),

                        // MEMBER
                        Map.of("actionKey", "mailchimp:member:createOrUpdate", "name", "Create or Update Member", "description", "Create a new member, or update the current one if it already exists (upsert)", "configSchema", List.of(
                                Map.of("key", "listId", "label", "List / Audience ID", "type", "text", "required", true),
                                Map.of("key", "email", "label", "Email", "type", "text", "required", true),
                                Map.of("key", "status", "label", "Status (subscribed, unsubscribed, cleaned, pending, transactional)", "type", "text", "required", true),
                                Map.of("key", "mergeFields", "label", "Merge Fields (JSON: FNAME, LNAME, etc.)", "type", "json"),
                                Map.of("key", "additionalFields", "label", "Additional Fields (JSON)", "type", "json")
                        )),
                        Map.of("actionKey", "mailchimp:member:delete", "name", "Delete Member", "description", "Delete a member from a list", "configSchema", List.of(
                                Map.of("key", "listId", "label", "List / Audience ID", "type", "text", "required", true),
                                Map.of("key", "email", "label", "Email", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "mailchimp:member:get", "name", "Get Member", "description", "Get information about a specific list member", "configSchema", List.of(
                                Map.of("key", "listId", "label", "List / Audience ID", "type", "text", "required", true),
                                Map.of("key", "email", "label", "Email", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "mailchimp:member:getAll", "name", "Get All Members", "description", "Get information about members in a list", "configSchema", List.of(
                                Map.of("key", "listId", "label", "List / Audience ID", "type", "text", "required", true),
                                Map.of("key", "status", "label", "Status Filter (subscribed, unsubscribed, cleaned, pending, transactional)", "type", "text"),
                                Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100)
                        )),
                        Map.of("actionKey", "mailchimp:member:update", "name", "Update Member", "description", "Update a member of a list", "configSchema", List.of(
                                Map.of("key", "listId", "label", "List / Audience ID", "type", "text", "required", true),
                                Map.of("key", "email", "label", "Email", "type", "text", "required", true),
                                Map.of("key", "status", "label", "Status (subscribed, unsubscribed, cleaned, pending, transactional)", "type", "text"),
                                Map.of("key", "mergeFields", "label", "Merge Fields (JSON: FNAME, LNAME, etc.)", "type", "json"),
                                Map.of("key", "additionalFields", "label", "Additional Fields (JSON)", "type", "json")
                        )),

                        // MEMBER TAG
                        Map.of("actionKey", "mailchimp:memberTag:add", "name", "Add Tags to Member", "description", "Add tags to a list member", "configSchema", List.of(
                                Map.of("key", "listId", "label", "List / Audience ID", "type", "text", "required", true),
                                Map.of("key", "email", "label", "Email", "type", "text", "required", true),
                                Map.of("key", "tags", "label", "Tags (comma separated)", "type", "text", "required", true)
                        )),
                        Map.of("actionKey", "mailchimp:memberTag:remove", "name", "Remove Tags from Member", "description", "Remove tags from a list member", "configSchema", List.of(
                                Map.of("key", "listId", "label", "List / Audience ID", "type", "text", "required", true),
                                Map.of("key", "email", "label", "Email", "type", "text", "required", true),
                                Map.of("key", "tags", "label", "Tags (comma separated)", "type", "text", "required", true)
                        ))
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true)
        )).altAuthType(AuthType.OAUTH2).category("marketing");
    }
}
