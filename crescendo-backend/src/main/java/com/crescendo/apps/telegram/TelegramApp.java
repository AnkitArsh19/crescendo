package com.crescendo.apps.telegram;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TelegramApp implements AppDefinition {

    @Override
    public App toApp() {
        var chatField = Map.of("key", "chatId", "label", "Chat",
                "type", "dynamic_dropdown", "resourceType", "chats",
                "required", true,
                "helpText", "Select the chat or group");

        return new App("telegram", "Telegram", "Send messages, media, and watch for incoming messages via a Telegram bot",
                "/icons/telegram.svg", AuthType.APIKEY,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-message",
                        "name", "New Message",
                        "description", "Triggers when the bot receives a new message",
                        "configSchema", List.of(chatField)
                    ),
                    Map.of(
                        "triggerKey", "new-callback-query",
                        "name", "New Callback Query",
                        "description", "Triggers when a user presses an inline keyboard button",
                        "configSchema", List.of()
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "send-message",
                        "name", "Send Message",
                        "description", "Send a text message to a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "text", "label", "Message",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Hello from Crescendo!",
                                   "helpText", "The message text to send"),
                            Map.of("key", "parseMode", "label", "Parse Mode",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(
                                       Map.of("value", "Markdown", "label", "Markdown"),
                                       Map.of("value", "HTML", "label", "HTML"),
                                       Map.of("value", "", "label", "Plain Text")
                                   ),
                                   "helpText", "Message formatting mode")
                        )
                    ),
                    Map.of(
                        "actionKey", "send-photo",
                        "name", "Send Photo",
                        "description", "Send a photo to a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "photo", "label", "Photo URL",
                                   "type", "text", "required", true,
                                   "placeholder", "https://example.com/image.jpg",
                                   "helpText", "URL of the photo to send"),
                            Map.of("key", "caption", "label", "Caption",
                                   "type", "text", "required", false,
                                   "helpText", "Optional photo caption")
                        )
                    ),
                    Map.of(
                        "actionKey", "send-document",
                        "name", "Send Document",
                        "description", "Send a file/document to a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "document", "label", "Document URL",
                                   "type", "text", "required", true,
                                   "placeholder", "https://example.com/report.pdf",
                                   "helpText", "URL of the document to send"),
                            Map.of("key", "caption", "label", "Caption",
                                   "type", "text", "required", false,
                                   "helpText", "Optional document caption")
                        )
                    )
                )
        )
        .credentialSchema(List.of(
            Map.of("key", "botToken", "label", "Bot Token",
                    "type", "password", "required", true,
                    "placeholder", "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11",
                    "helpText", "Create a bot via @BotFather on Telegram and copy the token")
        ))
        .category("communication")
        .helpUrl("https://core.telegram.org/bots#botfather");
    }
}
