package com.crescendo.catalog;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.resource.ResourceProvider;
import com.crescendo.execution.test.OperationTestContractFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.mockito.Answers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural safety net for the compiled app catalog.
 *
 * <p>This deliberately validates the definitions that {@code DataSeeder} will
 * publish, instead of parsing source text or accepting a partially discovered
 * set. It catches the regressions that would otherwise reach the workflow
 * builder: an advertised action with no handler, a dynamic dropdown with no
 * provider/resource type, malformed schemas, and duplicate catalog keys.</p>
 * <p>A contract test proves that all actions, triggers, and resources are correctly wired
 * within Crescendo's execution engine without requiring live credentials or internet access.</p>
 */
class CatalogContractTest {

    private static final String APPS_PACKAGE = "com.crescendo.apps";

    private static List<AppDefinition> appDefinitions;
    private static Map<String, App> appsByKey;
    private static Map<String, List<HandlerLocation>> actionHandlers;
    private static Map<String, ResourceProvider> resourceProviders;

    @BeforeAll
    static void discoverCompiledCatalog() {
        Set<Class<?>> appClasses = scanClasses();

        List<Class<? extends AppDefinition>> definitionClasses = new ArrayList<>();
        for (Class<?> type : appClasses) {
            if (type != AppDefinition.class
                    && AppDefinition.class.isAssignableFrom(type)
                    && !type.isInterface()
                    && !Modifier.isAbstract(type.getModifiers())) {
                definitionClasses.add(type.asSubclass(AppDefinition.class));
            }
        }
        definitionClasses.sort(java.util.Comparator.comparing(Class::getName));

        assertFalse(definitionClasses.isEmpty(), "No AppDefinition implementations were discovered");
        List<AppDefinition> discoveredDefinitions = new ArrayList<>();
        for (Class<? extends AppDefinition> definitionClass : definitionClasses) {
            discoveredDefinitions.add(instantiateNoArg(definitionClass));
        }
        appDefinitions = List.copyOf(discoveredDefinitions);

        appsByKey = new LinkedHashMap<>();
        for (AppDefinition definition : appDefinitions) {
            App app = Objects.requireNonNull(definition.toApp(),
                    () -> definition.getClass().getName() + " returned null from toApp()");
            String appKey = requireText(app.getAppKey(), definition.getClass().getName() + ".appKey");
            App previous = appsByKey.putIfAbsent(appKey, app);
            assertTrue(previous == null, () -> "Duplicate appKey '" + appKey + "'");
        }

        actionHandlers = discoverActionHandlers(appClasses);
        resourceProviders = appClasses.stream()
                .filter(ResourceProvider.class::isAssignableFrom)
                .filter(type -> type != ResourceProvider.class)
                .filter(type -> !type.isInterface() && !Modifier.isAbstract(type.getModifiers()))
                .map(type -> type.asSubclass(ResourceProvider.class))
                .map(CatalogContractTest::instantiateResourceProvider)
                .collect(Collectors.toMap(
                        ResourceProvider::appKey,
                        provider -> provider,
                        (left, right) -> {
                            throw new AssertionError("Multiple ResourceProviders declare appKey '" + left.appKey() + "'");
                        },
                        LinkedHashMap::new));
    }

    @Test
    @DisplayName("Every compiled AppDefinition is constructible and represented in the catalog")
    void everyAppDefinitionIsDiscoveredWithoutSilentSkips() {
        assertFalse(appDefinitions.isEmpty());
        assertTrue(appDefinitions.stream().allMatch(definition -> {
            App app = definition.toApp();
            return app != null && appsByKey.containsKey(app.getAppKey());
        }));
    }

    @Test
    @DisplayName("Catalog operations have unique keys and valid UI schemas")
    void catalogOperationsHaveConsistentSchemas() {
        for (App app : appsByKey.values()) {
            validateOperations(app, app.getTriggers(), "triggerKey", "trigger");
            validateOperations(app, app.getActions(), "actionKey", "action");
            validateSchema(app, "credentialSchema", app.getCredentialSchema());
        }
    }

    @Test
    @DisplayName("Every catalog action has exactly one executable ActionHandler")
    void advertisedActionsAreExecutable() {
        List<String> violations = new ArrayList<>();
        for (App app : appsByKey.values()) {
            for (Map<String, Object> action : safeList(app.getActions())) {
                String actionKey = requireText(action.get("actionKey"), app.getAppKey() + ".actionKey");
                String compositeKey = actionKey(app.getAppKey(), actionKey);
                List<HandlerLocation> matches = actionHandlers.getOrDefault(compositeKey, List.of());
                if (matches.size() != 1) {
                    violations.add("Catalog action '" + compositeKey
                            + "' must have exactly one handler; found " + matches);
                }
            }
        }

        for (Map.Entry<String, List<HandlerLocation>> entry : actionHandlers.entrySet()) {
            assertTrue(entry.getValue().size() == 1,
                    () -> "Duplicate ActionMapping '" + entry.getKey() + "': " + entry.getValue());
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    @Test
    @DisplayName("Every dynamic dropdown resolves to a matching ResourceProvider resource type")
    void dynamicDropdownsAreBackedByResourceProviders() {
        for (App app : appsByKey.values()) {
            validateDynamicFields(app, "trigger", app.getTriggers());
            validateDynamicFields(app, "action", app.getActions());
        }
    }

    @Test
    @DisplayName("Every catalog operation receives a safe test contract")
    void everyOperationHasASafeTestContract() {
        OperationTestContractFactory factory = new OperationTestContractFactory();
        for (App app : appsByKey.values()) {
            for (Map<String, Object> trigger : safeList(app.getTriggers())) {
                assertSafeTestContract(factory.create(app.getAppKey(), trigger, true), app.getAppKey(), trigger);
            }
            for (Map<String, Object> action : safeList(app.getActions())) {
                assertSafeTestContract(factory.create(app.getAppKey(), action, false), app.getAppKey(), action);
            }
        }
    }

    private static void assertSafeTestContract(Map<String, Object> contract, String appKey, Map<String, Object> operation) {
        String name = appKey + ":" + operation;
        assertTrue(contract.containsKey("setupPolicy"), () -> "Missing setupPolicy for " + name);
        assertTrue(contract.containsKey("checks"), () -> "Missing checks for " + name);
        assertTrue(contract.containsKey("liveTestAllowed"), () -> "Missing liveTestAllowed for " + name);
        assertTrue(contract.containsKey("sideEffect"), () -> "Missing sideEffect for " + name);
        assertFalse("LIVE_RUN".equals(contract.get("setupPolicy")), () -> "Setup must never default to live execution for " + name);
    }

    private static void validateOperations(App app, List<Map<String, Object>> operations,
                                           String keyField, String operationKind) {
        Set<String> operationKeys = new HashSet<>();
        for (Map<String, Object> operation : safeList(operations)) {
            String operationKey = requireText(operation.get(keyField), app.getAppKey() + "." + operationKind + "." + keyField);
            assertTrue(operationKeys.add(operationKey),
                    () -> "Duplicate " + operationKind + " key '" + operationKey + "' in app '" + app.getAppKey() + "'");
            requireText(operation.get("name"), app.getAppKey() + "." + operationKind + " '" + operationKey + "'.name");
            validateSchema(app, operationKind + " '" + operationKey + "' configSchema", schema(operation));
        }
    }

    private static void validateSchema(App app, String schemaName, List<Map<String, Object>> schema) {
        Set<String> fieldKeys = new HashSet<>();
        for (Map<String, Object> field : safeList(schema)) {
            String key = requireText(field.get("key"), app.getAppKey() + "." + schemaName + ".field.key");
            assertTrue(fieldKeys.add(key),
                    () -> "Duplicate field '" + key + "' in " + app.getAppKey() + "." + schemaName);
            requireText(field.get("label"), app.getAppKey() + "." + schemaName + "." + key + ".label");
            requireText(field.get("type"), app.getAppKey() + "." + schemaName + "." + key + ".type");
            Object required = field.get("required");
            assertTrue(required == null || required instanceof Boolean,
                    () -> app.getAppKey() + "." + schemaName + "." + key + ".required must be boolean when supplied");

            if (field.containsKey("options")) {
                Object options = field.get("options");
                assertTrue(options instanceof Collection<?>,
                        () -> app.getAppKey() + "." + schemaName + "." + key + ".options must be an array");
                for (Object option : (Collection<?>) options) {
                    if (option instanceof String label && !label.isBlank()) continue;
                    assertTrue(option instanceof Map<?, ?>,
                            () -> app.getAppKey() + "." + schemaName + "." + key + ".options entries must be non-blank strings or objects");
                    Map<?, ?> optionMap = (Map<?, ?>) option;
                    assertTrue(optionMap.containsKey("value"),
                            () -> app.getAppKey() + "." + schemaName + "." + key + ".option.value is required");
                    requireText(optionMap.get("label"), app.getAppKey() + "." + schemaName + "." + key + ".option.label");
                }
            }
        }
    }

    private static void validateDynamicFields(App app, String operationKind, List<Map<String, Object>> operations) {
        for (Map<String, Object> operation : safeList(operations)) {
            String operationKey = String.valueOf(operation.getOrDefault(operationKind.equals("action") ? "actionKey" : "triggerKey", ""));
            List<Map<String, Object>> schema = schema(operation);
            Set<String> fieldKeys = schema.stream().map(field -> String.valueOf(field.get("key"))).collect(Collectors.toSet());
            for (Map<String, Object> field : schema) {
                if (!"dynamic_dropdown".equals(field.get("type"))) continue;
                String fieldKey = requireText(field.get("key"), app.getAppKey() + "." + operationKind + ".field.key");
                String resourceType = requireText(field.get("resourceType"), app.getAppKey() + ":" + operationKey + "." + fieldKey + ".resourceType");
                ResourceProvider provider = resourceProviders.get(app.getAppKey());
                assertTrue(provider != null,
                        () -> "Dynamic dropdown '" + app.getAppKey() + ":" + operationKey + "." + fieldKey
                                + "' has no ResourceProvider");
                assertTrue(provider.supportedResourceTypes().contains(resourceType),
                        () -> "Dynamic dropdown '" + app.getAppKey() + ":" + operationKey + "." + fieldKey
                                + "' requests unsupported resource type '" + resourceType + "'. Provider supports "
                                + provider.supportedResourceTypes());

                Object dependsOn = field.get("dependsOn");
                if (dependsOn instanceof String dependency) {
                    assertTrue(fieldKeys.contains(dependency),
                            () -> "Dynamic dropdown '" + app.getAppKey() + ":" + operationKey + "." + fieldKey
                                    + "' depends on unknown field '" + dependency + "'");
                } else if (dependsOn instanceof Collection<?> dependencies) {
                    for (Object dependency : dependencies) {
                        assertTrue(dependency instanceof String && fieldKeys.contains(dependency),
                                () -> "Dynamic dropdown '" + app.getAppKey() + ":" + operationKey + "." + fieldKey
                                        + "' depends on unknown field '" + dependency + "'");
                    }
                } else {
                    assertTrue(dependsOn == null,
                            () -> "Dynamic dropdown '" + app.getAppKey() + ":" + operationKey + "." + fieldKey
                                    + "'.dependsOn must be a field key or array of field keys");
                }
            }
        }
    }

    private static Map<String, List<HandlerLocation>> discoverActionHandlers(Set<Class<?>> classes) {
        Map<String, List<HandlerLocation>> mappings = new HashMap<>();
        for (Class<?> type : classes) {
            ActionMapping classMapping = type.getAnnotation(ActionMapping.class);
            if (classMapping != null) {
                assertTrue(ActionHandler.class.isAssignableFrom(type),
                        () -> type.getName() + " has class-level @ActionMapping but does not implement ActionHandler");
                addMapping(mappings, classMapping, new HandlerLocation(type.getName(), "execute"));
            }
            for (Method method : type.getDeclaredMethods()) {
                ActionMapping mapping = method.getAnnotation(ActionMapping.class);
                if (mapping == null) continue;
                assertTrue(method.getParameterCount() == 1 && method.getParameterTypes()[0] == ActionContext.class,
                        () -> type.getName() + "#" + method.getName() + " must accept exactly one ActionContext");
                assertTrue(method.getReturnType() != Void.TYPE,
                        () -> type.getName() + "#" + method.getName() + " must return an action result");
                addMapping(mappings, mapping, new HandlerLocation(type.getName(), method.getName()));
            }
        }
        return mappings;
    }

    private static void addMapping(Map<String, List<HandlerLocation>> mappings,
                                   ActionMapping mapping, HandlerLocation location) {
        String key = actionKey(requireText(mapping.appKey(), location + ".appKey"),
                requireText(mapping.actionKey(), location + ".actionKey"));
        mappings.computeIfAbsent(key, ignored -> new ArrayList<>()).add(location);
    }

    private static Set<Class<?>> scanClasses() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(APPS_PACKAGE);
        return candidates.stream().map(BeanDefinition::getBeanClassName).map(className -> {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException exception) {
                throw new AssertionError("Could not load catalog class " + className, exception);
            }
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static <T> T instantiateNoArg(Class<T> type) {
        return assertDoesNotThrow(() -> {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        }, () -> "Catalog contract requires a no-arg constructible " + type.getName());
    }

    /**
     * Resource providers increasingly receive HTTP/token collaborators through
     * constructor injection. Their catalog metadata methods are deliberately
     * pure, so a CALLS_REAL_METHODS mock lets this structural suite validate
     * appKey/resource types without constructing a network client.
     */
    private static ResourceProvider instantiateResourceProvider(Class<? extends ResourceProvider> type) {
        try {
            Constructor<? extends ResourceProvider> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException ignored) {
            return org.mockito.Mockito.mock(type, Answers.CALLS_REAL_METHODS);
        } catch (Exception exception) {
            throw new AssertionError("Could not inspect ResourceProvider " + type.getName(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> schema(Map<String, Object> operation) {
        Object rawSchema = operation.get("configSchema");
        assertTrue(rawSchema == null || rawSchema instanceof List<?>,
                () -> "configSchema must be an array for " + operation);
        if (rawSchema == null) return List.of();
        List<?> values = (List<?>) rawSchema;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object value : values) {
            assertTrue(value instanceof Map<?, ?>, () -> "configSchema entries must be objects: " + value);
            result.add((Map<String, Object>) value);
        }
        return result;
    }

    private static List<Map<String, Object>> safeList(List<Map<String, Object>> value) {
        return value == null ? List.of() : value;
    }

    private static String requireText(Object value, String field) {
        assertTrue(value instanceof String && !((String) value).isBlank(), () -> field + " must be a non-blank string");
        return (String) value;
    }

    private static String actionKey(String appKey, String actionKey) {
        return appKey + ":" + actionKey;
    }

    private record HandlerLocation(String type, String method) {
        @Override public String toString() { return type + "#" + method; }
    }
}
