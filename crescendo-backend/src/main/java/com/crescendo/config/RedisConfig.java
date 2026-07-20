package com.crescendo.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.crescendo.shared.infrastructure.sse.WorkflowSseService;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis configuration for caching, queues (Streams), and general key-value operations.
 * Spring Boot autoconfigures RedisConnectionFactory from spring.data.redis.* properties.
 *
 * Uses Jackson 3 (tools.jackson) with JacksonJsonRedisSerializer (Spring Data Redis 4.0+).
 */
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * Gracefully handles cache read/write errors caused by stale or incompatible
     * serialized entries (common during development when serializer config changes).
     * Instead of throwing a 500, evicts the corrupted entry and falls through to the database.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache GET failed for key [{}] in cache [{}] — evicting stale entry: {}",
                        key, cache.getName(), ex.getMessage());
                try {
                    cache.evict(key);
                } catch (RuntimeException evictEx) {
                    log.debug("Eviction also failed for key [{}]: {}", key, evictEx.getMessage());
                }
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed for key [{}] in cache [{}]: {}",
                        key, cache.getName(), ex.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache EVICT failed for key [{}] in cache [{}]: {}",
                        key, cache.getName(), ex.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("Cache CLEAR failed for cache [{}]: {}", cache.getName(), ex.getMessage());
            }
        };
    }

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

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            WorkflowSseService workflowSseService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(workflowSseService, new PatternTopic("workflow-events:*"));
        return container;
    }

    /**
     * Cache manager with per-cache TTL configuration.
     * Default TTL is 30 minutes; specific caches can have custom TTLs.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        JacksonJsonRedisSerializer<Object> jsonSerializer =
                new JacksonJsonRedisSerializer<>(redisObjectMapper(), Object.class);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("v3:")   // orphans v2 keys written with incompatible type-info format
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                "users",         defaultConfig.entryTtl(jitter(Duration.ofMinutes(15))),
                "workflows",     defaultConfig.entryTtl(jitter(Duration.ofMinutes(30))),
                "steps",         defaultConfig.entryTtl(jitter(Duration.ofMinutes(30))),
                "workflowLists", defaultConfig.entryTtl(Duration.ofSeconds(60)),  // short-lived, no jitter needed
                "accessTiers",   defaultConfig.entryTtl(Duration.ofMinutes(5)),
                "workflowRuns",  defaultConfig.entryTtl(Duration.ofHours(1)),
                // Use JSON serialization for app catalog to prevent JDK serialization exceptions
                "apps",          defaultConfig.entryTtl(jitter(Duration.ofHours(6)))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Applies a ±10% random jitter to the given base TTL to prevent cache avalanche.
     *
     * <p>Without jitter, entries populated at the same moment (e.g. after a cold start
     * or deploy) all expire simultaneously, causing a sudden spike of DB queries.
     * Spreading expirations over a ±10% window distributes that load evenly.
     *
     * <p>Uses {@link ThreadLocalRandom} to avoid contention on a shared {@code Random}.
     */
    private static Duration jitter(Duration base) {
        long millis = base.toMillis();
        long offset = (long) (millis * 0.10); // ±10% of base
        long jitterMs = ThreadLocalRandom.current().nextLong(-offset, offset + 1);
        return Duration.ofMillis(Math.max(1, millis + jitterMs));
    }
}

