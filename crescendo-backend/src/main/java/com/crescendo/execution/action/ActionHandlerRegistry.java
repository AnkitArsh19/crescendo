package com.crescendo.execution.action;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring-managed registry that indexes all {@link ActionHandler} beans
 * by their {@link ActionMapping} annotation.
 *
 * <p>Supports two patterns:
 * <ol>
 *   <li><b>Class-level</b>: A class that implements {@link ActionHandler} and is annotated
 *       with {@code @ActionMapping} at the class level. The {@code execute()} method is called
 *       directly.</li>
 *   <li><b>Method-level</b>: A plain {@code @Component} class (grouped handler) where
 *       individual methods are annotated with {@code @ActionMapping}. The registry wraps
 *       each such method in a lightweight {@link ActionHandler} adapter.</li>
 * </ol>
 *
 * <p>Look-up is O(1) via the composite key {@code appKey:actionKey}.
 *
 * <p>The method-level scan is deferred to {@code @PostConstruct} to avoid
 * circular dependency issues (the constructor calling {@code getBean()} on
 * every bean can trigger premature instantiation of beans that depend on
 * this registry).
 */
@Component
public class ActionHandlerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ActionHandlerRegistry.class);

    private final Map<String, ActionHandler> handlers = new ConcurrentHashMap<>();
    private final List<ActionHandler> classLevelHandlers;
    private final ApplicationContext applicationContext;

    public ActionHandlerRegistry(List<ActionHandler> classLevelHandlers,
                                 ApplicationContext applicationContext) {
        this.classLevelHandlers = classLevelHandlers;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    void init() {
        // ── Pattern 1: class-level @ActionMapping (implements ActionHandler) ──
        for (ActionHandler handler : classLevelHandlers) {
            ActionMapping mapping = handler.getClass().getAnnotation(ActionMapping.class);
            if (mapping == null) {
                logger.warn("ActionHandler {} has no @ActionMapping — skipping registration",
                        handler.getClass().getSimpleName());
                continue;
            }
            String key = toKey(mapping.appKey(), mapping.actionKey());
            handlers.put(key, handler);
            logger.debug("Registered class-level handler: {} → {}", key, handler.getClass().getSimpleName());
        }

        // ── Pattern 2: method-level @ActionMapping on plain @Component beans ──
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e) {
                // Skip beans that cannot be instantiated (e.g. abstract, prototype scope)
                continue;
            }
            // Skip beans that are already registered as class-level ActionHandlers
            if (bean instanceof ActionHandler) continue;

            Class<?> beanClass = bean.getClass();
            // Walk through declared methods (also handles CGLIB proxies via getSuperclass)
            Class<?> inspected = beanClass;
            while (inspected != null && inspected != Object.class) {
                for (Method method : inspected.getDeclaredMethods()) {
                    ActionMapping mapping = method.getAnnotation(ActionMapping.class);
                    if (mapping == null) continue;

                    String key = toKey(mapping.appKey(), mapping.actionKey());
                    if (handlers.containsKey(key)) {
                        logger.warn("Duplicate handler key '{}' — {} will be ignored in favour of existing registration",
                                key, method.toGenericString());
                        continue;
                    }

                    method.setAccessible(true);
                    final Object targetBean = bean;
                    final Method targetMethod = method;

                    ActionHandler adapter = context -> {
                        try {
                            Object result = targetMethod.invoke(targetBean, context);
                            if (result instanceof ActionResult ar) {
                                return ar;
                            }
                            return ActionResult.success(result);
                        } catch (Exception e) {
                            Throwable cause = e.getCause() != null ? e.getCause() : e;
                            logger.error("Method handler '{}' threw an exception", key, cause);
                            return ActionResult.failure(cause.getMessage());
                        }
                    };

                    handlers.put(key, adapter);
                    logger.debug("Registered method-level handler: {} → {}#{}", key,
                            beanClass.getSimpleName(), method.getName());
                }
                inspected = inspected.getSuperclass();
            }
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

