package com.crescendo.execution.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring-managed registry that indexes all {@link ResourceProvider} beans by their
 * {@link ResourceProvider#appKey()}.
 * <p>
 * Look-up is O(1) via the app key. Same pattern as {@link com.crescendo.execution.action.ActionHandlerRegistry}.
 */
@Component
public class ResourceProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ResourceProviderRegistry.class);

    private final Map<String, ResourceProvider> providers = new ConcurrentHashMap<>();

    public ResourceProviderRegistry(List<ResourceProvider> providerBeans) {
        for (ResourceProvider provider : providerBeans) {
            String key = provider.appKey();
            if (key == null || key.isBlank()) {
                logger.warn("ResourceProvider {} returned blank appKey — skipping",
                        provider.getClass().getSimpleName());
                continue;
            }
            providers.put(key, provider);
            logger.info("Registered resource provider: {} → {} (types: {})",
                    key, provider.getClass().getSimpleName(), provider.supportedResourceTypes());
        }
        logger.info("ResourceProviderRegistry initialised with {} provider(s)", providers.size());
    }

    /**
     * Finds the resource provider for the given app key.
     */
    public Optional<ResourceProvider> find(String appKey) {
        return Optional.ofNullable(providers.get(appKey));
    }

    /**
     * Returns {@code true} if a resource provider is registered for the given app key.
     */
    public boolean hasProvider(String appKey) {
        return providers.containsKey(appKey);
    }
}
