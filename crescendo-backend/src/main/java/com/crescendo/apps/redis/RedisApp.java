package com.crescendo.apps.redis;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RedisApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("redis", "Redis", "Run simple commands against a user-owned Redis server",
                "/icons/redis.svg", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "run-command", "name", "Run Command",
                                "description", "Run a Redis command such as GET, SET, DEL, LPUSH, HGET, or PUBLISH",
                                "configSchema", List.of(
                                        Map.of("key", "command", "label", "Command", "type", "text", "required", true,
                                                "placeholder", "GET my-key"),
                                        Map.of("key", "args", "label", "Arguments (JSON Array)", "type", "json", "required", false,
                                                "placeholder", "[\"my-key\"]")))
                )
        ).credentialSchema(List.of(
                Map.of("key", "host", "label", "Host", "type", "text", "required", true,
                        "placeholder", "localhost"),
                Map.of("key", "port", "label", "Port", "type", "text", "required", false,
                        "placeholder", "6379"),
                Map.of("key", "password", "label", "Password", "type", "password", "required", false),
                Map.of("key", "database", "label", "Database", "type", "text", "required", false,
                        "placeholder", "0")
        )).category("database").helpUrl("https://redis.io/docs/latest/commands/");
    }
}
