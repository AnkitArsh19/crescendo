package com.crescendo.execution.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring-managed registry that indexes all {@link ActionHandler} beans
 * by their {@link ActionMapping} annotation.
 * <p>
 * Look-up is O(1) via the composite key {@code appKey:actionKey}.
 */
@Component
public class ActionHandlerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ActionHandlerRegistry.class);

    private final Map<String, ActionHandler> handlers = new ConcurrentHashMap<>();

    public ActionHandlerRegistry(List<ActionHandler> handlerBeans) {
        for (ActionHandler handler : handlerBeans) {
            ActionMapping mapping = handler.getClass().getAnnotation(ActionMapping.class);
            if (mapping == null) {
                logger.warn("ActionHandler {} has no @ActionMapping — skipping registration",
                        handler.getClass().getSimpleName());
                continue;
            }
            String key = toKey(mapping.appKey(), mapping.actionKey());
            handlers.put(key, handler);
            logger.info("Registered action handler: {} → {}", key, handler.getClass().getSimpleName());
        }
        logger.info("ActionHandlerRegistry initialised with {} handler(s)", handlers.size());
    }

    public Optional<ActionHandler> find(String appKey, String actionKey) {
        return Optional.ofNullable(handlers.get(toKey(appKey, actionKey)));
    }

    public boolean hasHandler(String appKey, String actionKey) {
        return handlers.containsKey(toKey(appKey, actionKey));
    }

    private static String toKey(String appKey, String actionKey) {
        return appKey + ":" + actionKey;
    }
}
