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

        return new App("telegram", "Telegram", """
                Telegram is a cloud-based mobile and desktop messaging app with a focus on security and speed. The Crescendo Telegram app lets you build powerful chat bots and automated notification systems.

                **What you can do with Telegram in Crescendo:**
                - Send text messages, photos, documents, and locations to chats or channels
                - Monitor groups for specific keywords or commands
                - Build interactive bots using Callback Queries
                - Broadcast alerts like server downtime or new sales directly to your phone

                **Triggers available:**
                - New Message — trigger a workflow when someone messages your bot
                - New Callback Query — respond to inline button clicks

                **Who should use this:** Developers building chatbots, marketers sending broadcast messages to channels, and individuals wanting real-time alerts on their mobile devices.

                **Authentication:** API Key (create a bot via @BotFather).
                """,
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
                        "actionKey", "sendMessage",
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
                                   "helpText", "Message formatting mode"),
                            Map.of("key", "replyToMessageId", "label", "Reply To Message ID",
                                   "type", "text", "required", false,
                                   "helpText", "If provided, replies to a specific message"),
                            Map.of("key", "disableNotification", "label", "Disable Notification",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "replyMarkup", "label", "Reply Markup (JSON)",
                                   "type", "textarea", "required", false,
                                   "helpText", "JSON object for an inline keyboard, custom reply keyboard, etc.")
                        )
                    ),
                    Map.of(
                        "actionKey", "sendPhoto",
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
                                   "helpText", "Optional photo caption"),
                            Map.of("key", "replyToMessageId", "label", "Reply To Message ID", "type", "text", "required", false),
                            Map.of("key", "disableNotification", "label", "Disable Notification", "type", "dropdown", "required", false, "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "replyMarkup", "label", "Reply Markup (JSON)", "type", "textarea", "required", false)
                        )
                    ),
                    Map.of(
                        "actionKey", "sendDocument",
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
                                   "helpText", "Optional document caption"),
                            Map.of("key", "replyToMessageId", "label", "Reply To Message ID", "type", "text", "required", false),
                            Map.of("key", "disableNotification", "label", "Disable Notification", "type", "dropdown", "required", false, "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "replyMarkup", "label", "Reply Markup (JSON)", "type", "textarea", "required", false)
                        )
                    ),
                    Map.of(
                        "actionKey", "sendLocation",
                        "name", "Send Location",
                        "description", "Send a GPS location to a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "latitude", "label", "Latitude",
                                   "type", "text", "required", true,
                                   "placeholder", "28.6139",
                                   "helpText", "GPS latitude coordinate"),
                            Map.of("key", "longitude", "label", "Longitude",
                                   "type", "text", "required", true,
                                   "placeholder", "77.2090",
                                   "helpText", "GPS longitude coordinate"),
                            Map.of("key", "replyToMessageId", "label", "Reply To Message ID", "type", "text", "required", false),
                            Map.of("key", "disableNotification", "label", "Disable Notification", "type", "dropdown", "required", false, "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "replyMarkup", "label", "Reply Markup (JSON)", "type", "textarea", "required", false)
                        )
                    ),
                    Map.of(
                        "actionKey", "sendAudio",
                        "name", "Send Audio",
                        "description", "Send an audio file to a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "audio", "label", "Audio URL", "type", "text", "required", true),
                            Map.of("key", "caption", "label", "Caption", "type", "text", "required", false),
                            Map.of("key", "replyToMessageId", "label", "Reply To Message ID", "type", "text", "required", false),
                            Map.of("key", "disableNotification", "label", "Disable Notification", "type", "dropdown", "required", false, "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "replyMarkup", "label", "Reply Markup (JSON)", "type", "textarea", "required", false)
                        )
                    ),
                    Map.of(
                        "actionKey", "sendVideo",
                        "name", "Send Video",
                        "description", "Send a video to a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "video", "label", "Video URL", "type", "text", "required", true),
                            Map.of("key", "caption", "label", "Caption", "type", "text", "required", false),
                            Map.of("key", "replyToMessageId", "label", "Reply To Message ID", "type", "text", "required", false),
                            Map.of("key", "disableNotification", "label", "Disable Notification", "type", "dropdown", "required", false, "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "replyMarkup", "label", "Reply Markup (JSON)", "type", "textarea", "required", false)
                        )
                    ),
                    Map.of(
                        "actionKey", "sendAnimation",
                        "name", "Send Animation",
                        "description", "Send an animation (GIF) to a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "animation", "label", "Animation URL", "type", "text", "required", true),
                            Map.of("key", "caption", "label", "Caption", "type", "text", "required", false),
                            Map.of("key", "replyToMessageId", "label", "Reply To Message ID", "type", "text", "required", false),
                            Map.of("key", "disableNotification", "label", "Disable Notification", "type", "dropdown", "required", false, "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "replyMarkup", "label", "Reply Markup (JSON)", "type", "textarea", "required", false)
                        )
                    ),
                    Map.of(
                        "actionKey", "sendSticker",
                        "name", "Send Sticker",
                        "description", "Send a sticker to a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "sticker", "label", "Sticker URL", "type", "text", "required", true),
                            Map.of("key", "replyToMessageId", "label", "Reply To Message ID", "type", "text", "required", false),
                            Map.of("key", "disableNotification", "label", "Disable Notification", "type", "dropdown", "required", false, "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "replyMarkup", "label", "Reply Markup (JSON)", "type", "textarea", "required", false)
                        )
                    ),
                    Map.of(
                        "actionKey", "sendVoice",
                        "name", "Send Voice",
                        "description", "Send a voice note to a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "voice", "label", "Voice URL", "type", "text", "required", true),
                            Map.of("key", "caption", "label", "Caption", "type", "text", "required", false),
                            Map.of("key", "replyToMessageId", "label", "Reply To Message ID", "type", "text", "required", false),
                            Map.of("key", "disableNotification", "label", "Disable Notification", "type", "dropdown", "required", false, "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "replyMarkup", "label", "Reply Markup (JSON)", "type", "textarea", "required", false)
                        )
                    ),
                    Map.of(
                        "actionKey", "pinMessage",
                        "name", "Pin Message",
                        "description", "Pin a message in a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "messageId", "label", "Message ID", "type", "text", "required", true),
                            Map.of("key", "disableNotification", "label", "Disable Notification", "type", "dropdown", "required", false, "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False")))
                        )
                    ),
                    Map.of(
                        "actionKey", "unpinMessage",
                        "name", "Unpin Message",
                        "description", "Unpin a message in a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "messageId", "label", "Message ID", "type", "text", "required", false, "helpText", "Message ID to unpin. If empty, unpins the most recent message.")
                        )
                    ),
                    Map.of(
                        "actionKey", "deleteMessage",
                        "name", "Delete Message",
                        "description", "Delete a message in a Telegram chat",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "messageId", "label", "Message ID", "type", "text", "required", true)
                        )
                    ),
                    Map.of(
                        "actionKey", "editMessage",
                        "name", "Edit Message",
                        "description", "Edit an existing text message",
                        "configSchema", List.of(
                            chatField,
                            Map.of("key", "messageId", "label", "Message ID", "type", "text", "required", true),
                            Map.of("key", "text", "label", "New Text", "type", "textarea", "required", true),
                            Map.of("key", "parseMode", "label", "Parse Mode", "type", "dropdown", "required", false, "options", List.of(Map.of("value", "Markdown", "label", "Markdown"), Map.of("value", "HTML", "label", "HTML"), Map.of("value", "", "label", "Plain Text"))),
                            Map.of("key", "replyMarkup", "label", "Reply Markup (JSON)", "type", "textarea", "required", false)
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
