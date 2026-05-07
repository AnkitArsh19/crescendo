package com.crescendo.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis configuration for caching, queues (Streams), and general key-value operations.
 * Spring Boot autoconfigures RedisConnectionFactory from spring.data.redis.* properties.
 *
 * Uses Jackson 3 (tools.jackson) with JacksonJsonRedisSerializer (Spring Data Redis 4.0+).
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Jackson 3 JsonMapper configured for Redis serialization with type info embedded in JSON.
     * Polymorphic type info allows deserialization of cached objects back to their concrete types.
     * Java 8 date/time types are supported natively in Jackson 3 — no module registration needed.

    *
     * Uses WRAPPER_ARRAY (not PROPERTY) to avoid the nested-map token conflict:
     * PROPERTY injects a field inside the object which breaks when a Map value is itself a Map
     * (Jackson sees '{' where it expects a type-id string).
     * Validator is narrowed to only crescendo types + collections — not Object.class — to avoid
     * embedding type metadata into every primitive string/int in Map<String, Object> values.
     */
    private ObjectMapper redisObjectMapper() {
        return JsonMapper.builder()
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType("com.crescendo")
                                .allowIfSubType(java.util.List.class)
                                .allowIfSubType(java.util.Map.class)
                                .build(),
                        DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.WRAPPER_ARRAY
                )
                .build();
    }

    /**
     * General-purpose RedisTemplate for manual Redis operations (queues, pub/sub, raw key-value).
     * Uses String keys and JSON-serialized values for readability in RedisInsight.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        JacksonJsonRedisSerializer<Object> jsonSerializer =
                new JacksonJsonRedisSerializer<>(redisObjectMapper(), Object.class);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Cache manager with per-cache TTL configuration.
     * Default TTL is 30 minutes; specific caches can have custom TTLs.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        JacksonJsonRedisSerializer<Object> jsonSerializer =
                new JacksonJsonRedisSerializer<>(redisObjectMapper(), Object.class);
                JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("v2:")   // orphans all v1 keys written with old serializer format
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                "users", defaultConfig.entryTtl(Duration.ofMinutes(15)),
                "workflows", defaultConfig.entryTtl(Duration.ofMinutes(30)),
                "steps", defaultConfig.entryTtl(Duration.ofMinutes(30)),
                "accessTiers", defaultConfig.entryTtl(Duration.ofMinutes(5)),
                "workflowRuns", defaultConfig.entryTtl(Duration.ofHours(1)),
                // Dedicated serializer for app catalog to avoid JSON polymorphic type-wrapper edge cases.
                "apps", defaultConfig.entryTtl(Duration.ofHours(6))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jdkSerializer))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
