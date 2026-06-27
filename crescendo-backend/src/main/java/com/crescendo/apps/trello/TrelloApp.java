package com.crescendo.apps.trello;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Trello.
 *
 * Resources (from n8n Trello node):
 *   - attachment : create, delete, get, getAll
 *   - board      : create, delete, get, update
 *   - boardMember: add, getAll, remove
 *   - card       : create, delete, get, update
 *   - cardComment: create, delete, update
 *   - checklist  : create, delete, get, getAll
 *   - label      : create, delete, get, getAll, update
 *   - list       : create, get, getAll, update
 */
@Component
public class TrelloApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "trello",
                "Trello",
                """
                Trello is a web-based, Kanban-style, list-making application.
                
                This integration provides comprehensive operations for:
                - **Attachment**: Create, Delete, Get, Get All
                - **Board**: Create, Delete, Get, Update
                - **Board Member**: Add, Get All, Remove
                - **Card**: Create, Delete, Get, Update
                - **Card Comment**: Create, Delete, Update
                - **Checklist**: Create, Delete, Get, Get All
                - **Label**: Create, Delete, Get, Get All, Update
                - **List**: Create, Get, Get All, Update
                
                Authenticate using a Trello API Key and API Token, or OAuth2.
                """,
                "https://www.google.com/s2/favicons?domain=trello.com&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // ATTACHMENT
                        Map.of("actionKey", "trello:attachment:create", "name", "Create Attachment", "description", "Add attachment to a card", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true), Map.of("key", "url", "label", "File URL", "type", "text"), Map.of("key", "name", "label", "File Name", "type", "text"))),
                        Map.of("actionKey", "trello:attachment:delete", "name", "Delete Attachment", "description", "Delete an attachment", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true), Map.of("key", "attachmentId", "label", "Attachment ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:attachment:get", "name", "Get Attachment", "description", "Get an attachment", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true), Map.of("key", "attachmentId", "label", "Attachment ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:attachment:getAll", "name", "Get All Attachments", "description", "Get all attachments on a card", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true))),

                        // BOARD
                        Map.of("actionKey", "trello:board:create", "name", "Create Board", "description", "Create a board", "configSchema", List.of(Map.of("key", "name", "label", "Name", "type", "text", "required", true), Map.of("key", "description", "label", "Description", "type", "text"))),
                        Map.of("actionKey", "trello:board:delete", "name", "Delete Board", "description", "Delete a board", "configSchema", List.of(Map.of("key", "boardId", "label", "Board ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:board:get", "name", "Get Board", "description", "Get a board", "configSchema", List.of(Map.of("key", "boardId", "label", "Board ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:board:update", "name", "Update Board", "description", "Update a board", "configSchema", List.of(Map.of("key", "boardId", "label", "Board ID", "type", "text", "required", true), Map.of("key", "fields", "label", "Fields to update (JSON)", "type", "json", "required", true))),

                        // BOARD MEMBER
                        Map.of("actionKey", "trello:boardMember:add", "name", "Add Member to Board", "description", "Add a member to a board", "configSchema", List.of(Map.of("key", "boardId", "label", "Board ID", "type", "text", "required", true), Map.of("key", "memberId", "label", "Member ID", "type", "text", "required", true), Map.of("key", "type", "label", "Type (admin, normal, observer)", "type", "text"))),
                        Map.of("actionKey", "trello:boardMember:getAll", "name", "Get All Board Members", "description", "Get all members of a board", "configSchema", List.of(Map.of("key", "boardId", "label", "Board ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:boardMember:remove", "name", "Remove Member from Board", "description", "Remove a member from a board", "configSchema", List.of(Map.of("key", "boardId", "label", "Board ID", "type", "text", "required", true), Map.of("key", "memberId", "label", "Member ID", "type", "text", "required", true))),

                        // CARD
                        Map.of("actionKey", "trello:card:create", "name", "Create Card", "description", "Create a card", "configSchema", List.of(Map.of("key", "listId", "label", "List ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true), Map.of("key", "description", "label", "Description", "type", "text"))),
                        Map.of("actionKey", "trello:card:delete", "name", "Delete Card", "description", "Delete a card", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:card:get", "name", "Get Card", "description", "Get a card", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:card:update", "name", "Update Card", "description", "Update a card", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true), Map.of("key", "fields", "label", "Fields to update (JSON)", "type", "json", "required", true))),

                        // CARD COMMENT
                        Map.of("actionKey", "trello:cardComment:create", "name", "Create Comment", "description", "Add a comment to a card", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true), Map.of("key", "text", "label", "Text", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:cardComment:delete", "name", "Delete Comment", "description", "Delete a comment", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true), Map.of("key", "commentId", "label", "Comment ID (Action ID)", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:cardComment:update", "name", "Update Comment", "description", "Update a comment", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true), Map.of("key", "commentId", "label", "Comment ID", "type", "text", "required", true), Map.of("key", "text", "label", "Text", "type", "text", "required", true))),

                        // CHECKLIST
                        Map.of("actionKey", "trello:checklist:create", "name", "Create Checklist", "description", "Create a checklist on a card", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:checklist:delete", "name", "Delete Checklist", "description", "Delete a checklist", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true), Map.of("key", "checklistId", "label", "Checklist ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:checklist:get", "name", "Get Checklist", "description", "Get a checklist", "configSchema", List.of(Map.of("key", "checklistId", "label", "Checklist ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:checklist:getAll", "name", "Get All Checklists", "description", "Get all checklists on a card", "configSchema", List.of(Map.of("key", "cardId", "label", "Card ID", "type", "text", "required", true))),

                        // LABEL
                        Map.of("actionKey", "trello:label:create", "name", "Create Label", "description", "Create a label on a board", "configSchema", List.of(Map.of("key", "boardId", "label", "Board ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true), Map.of("key", "color", "label", "Color", "type", "text"))),
                        Map.of("actionKey", "trello:label:delete", "name", "Delete Label", "description", "Delete a label", "configSchema", List.of(Map.of("key", "labelId", "label", "Label ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:label:get", "name", "Get Label", "description", "Get a label", "configSchema", List.of(Map.of("key", "labelId", "label", "Label ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:label:getAll", "name", "Get All Labels", "description", "Get all labels on a board", "configSchema", List.of(Map.of("key", "boardId", "label", "Board ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:label:update", "name", "Update Label", "description", "Update a label", "configSchema", List.of(Map.of("key", "labelId", "label", "Label ID", "type", "text", "required", true), Map.of("key", "fields", "label", "Fields to update", "type", "json", "required", true))),

                        // LIST
                        Map.of("actionKey", "trello:list:create", "name", "Create List", "description", "Create a list on a board", "configSchema", List.of(Map.of("key", "boardId", "label", "Board ID", "type", "text", "required", true), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:list:get", "name", "Get List", "description", "Get a list", "configSchema", List.of(Map.of("key", "listId", "label", "List ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:list:getAll", "name", "Get All Lists", "description", "Get all lists on a board", "configSchema", List.of(Map.of("key", "boardId", "label", "Board ID", "type", "text", "required", true))),
                        Map.of("actionKey", "trello:list:update", "name", "Update List", "description", "Update a list", "configSchema", List.of(Map.of("key", "listId", "label", "List ID", "type", "text", "required", true), Map.of("key", "fields", "label", "Fields to update", "type", "json", "required", true)))
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiKey", "label", "API Key", "type", "text", "required", true),
                Map.of("key", "apiToken", "label", "API Token", "type", "password", "required", true)
        )).altAuthType(AuthType.OAUTH2).category("task-management");
    }
}
