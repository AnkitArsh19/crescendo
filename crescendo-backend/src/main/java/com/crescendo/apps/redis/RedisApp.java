package com.crescendo.apps.redis;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Redis.
 */
@Component
public class RedisApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "redis",
                "Redis",
                """
                Get, send and update data in Redis.
                
                This integration provides operations for:
                - **Delete**: Delete a key from Redis
                - **Get**: Get the value of a key from Redis
                - **Increment**: Atomically increment a key by 1
                - **Info**: Return generic information about the Redis instance
                - **Keys**: Return all keys matching a pattern
                - **List Length**: Return the length of a list
                - **Pop**: Pop data from a redis list
                - **Publish**: Publish message to redis channel
                - **Push**: Push data to a redis list
                - **Set**: Set the value of a key in redis
                """,
                "https://www.google.com/s2/favicons?domain=redis.io&sz=128", // Generic icon
                AuthType.OAUTH2, // Placeholder, redis uses connection string
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "redis:delete",
                                "name", "Delete",
                                "description", "Delete a key from Redis",
                                "configSchema", List.of(
                                        Map.of("key", "key", "label", "Key", "type", "text", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "redis:get",
                                "name", "Get",
                                "description", "Get the value of a key from Redis",
                                "configSchema", List.of(
                                        Map.of("key", "propertyName", "label", "Property Name", "type", "text", "default", "propertyName"),
                                        Map.of("key", "key", "label", "Key", "type", "text", "required", true),
                                        Map.of("key", "keyType", "label", "Key Type", "type", "text", "default", "automatic"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "redis:incr",
                                "name", "Increment",
                                "description", "Atomically increment a key by 1",
                                "configSchema", List.of(
                                        Map.of("key", "key", "label", "Key", "type", "text", "required", true),
                                        Map.of("key", "expire", "label", "Expire", "type", "boolean", "default", false),
                                        Map.of("key", "ttl", "label", "TTL", "type", "number", "default", 60)
                                )
                        ),
                        Map.of(
                                "actionKey", "redis:info",
                                "name", "Info",
                                "description", "Return generic information about the Redis instance",
                                "configSchema", List.of()
                        ),
                        Map.of(
                                "actionKey", "redis:keys",
                                "name", "Keys",
                                "description", "Return all keys matching a pattern",
                                "configSchema", List.of(
                                        Map.of("key", "keyPattern", "label", "Key Pattern", "type", "text", "required", true),
                                        Map.of("key", "getValues", "label", "Get Values", "type", "boolean", "default", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "redis:llen",
                                "name", "List Length",
                                "description", "Return the length of a list",
                                "configSchema", List.of(
                                        Map.of("key", "list", "label", "List", "type", "text", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "redis:pop",
                                "name", "Pop",
                                "description", "Pop data from a redis list",
                                "configSchema", List.of(
                                        Map.of("key", "list", "label", "List", "type", "text", "required", true),
                                        Map.of("key", "tail", "label", "Tail", "type", "boolean", "default", false),
                                        Map.of("key", "propertyName", "label", "Property Name", "type", "text", "default", "propertyName"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "redis:publish",
                                "name", "Publish",
                                "description", "Publish message to redis channel",
                                "configSchema", List.of(
                                        Map.of("key", "channel", "label", "Channel", "type", "text", "required", true),
                                        Map.of("key", "messageData", "label", "Data", "type", "text", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "redis:push",
                                "name", "Push",
                                "description", "Push data to a redis list",
                                "configSchema", List.of(
                                        Map.of("key", "list", "label", "List", "type", "text", "required", true),
                                        Map.of("key", "messageData", "label", "Data", "type", "text", "required", true),
                                        Map.of("key", "tail", "label", "Tail", "type", "boolean", "default", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "redis:set",
                                "name", "Set",
                                "description", "Set the value of a key in redis",
                                "configSchema", List.of(
                                        Map.of("key", "key", "label", "Key", "type", "text", "required", true),
                                        Map.of("key", "value", "label", "Value", "type", "text"),
                                        Map.of("key", "keyType", "label", "Key Type", "type", "text", "default", "automatic"),
                                        Map.of("key", "valueIsJSON", "label", "Value Is JSON", "type", "boolean", "default", true),
                                        Map.of("key", "expire", "label", "Expire", "type", "boolean", "default", false),
                                        Map.of("key", "ttl", "label", "TTL", "type", "number", "default", 60)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "host", "label", "Host", "type", "string", "required", true),
                Map.of("key", "port", "label", "Port", "type", "number", "required", true, "default", 6379),
                Map.of("key", "password", "label", "Password", "type", "string")
        )).category("databases");
    }
}
