package com.crescendo.execution.test;

import com.crescendo.app.App;
import com.crescendo.app.AppRepository;
import com.crescendo.connections.connections_command.Connections_command;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.oauth.OAuthTokenRefreshService;
import com.crescendo.enums.AuthType;
import com.crescendo.enums.ConnectionStatus;
import com.crescendo.execution.resource.ResourceFetchService;
import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.security.DataSanitizationService;
import com.crescendo.shared.domain.valueobject.AppKey;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Non-mutating setup checks shared by every workflow operation. */
@Service
public class StepSetupValidationService {

    public record SetupCheck(String id, String label, String status, String detail) {}

    public record SetupValidationResult(
            boolean success,
            List<SetupCheck> checks,
            Map<String, Object> preview,
            String error,
            Map<String, Object> testContract) {}

    private final AppRepository appRepository;
    private final Connections_commandRepository connectionRepository;
    private final OAuthTokenRefreshService tokenService;
    private final ResourceFetchService resourceFetchService;
    private final DataSanitizationService sanitizationService;
    private final OperationTestContractFactory contractFactory;
    private final com.crescendo.execution.expression.WorkflowExpressionResolver expressionResolver;

    public StepSetupValidationService(AppRepository appRepository,
                                      Connections_commandRepository connectionRepository,
                                      OAuthTokenRefreshService tokenService,
                                      ResourceFetchService resourceFetchService,
                                      DataSanitizationService sanitizationService,
                                      OperationTestContractFactory contractFactory,
                                      com.crescendo.execution.expression.WorkflowExpressionResolver expressionResolver) {
        this.appRepository = appRepository;
        this.connectionRepository = connectionRepository;
        this.tokenService = tokenService;
        this.resourceFetchService = resourceFetchService;
        this.sanitizationService = sanitizationService;
        this.contractFactory = contractFactory;
        this.expressionResolver = expressionResolver;
    }

    public SetupValidationResult validate(String appKey, String operationKey, boolean trigger,
                                          String connectionId, Map<String, Object> configuration,
                                          Map<String, Object> inputData, UUID userId) {
        List<SetupCheck> checks = new ArrayList<>();
        Map<String, Object> config = configuration == null ? Map.of() : configuration;
        Map<String, Object> input = inputData == null ? Map.of() : inputData;
        App app;
        Map<String, Object> operation;
        try {
            app = appRepository.findById(AppKey.of(appKey)).orElse(null);
            if (app == null) return failure(checks, "app", "App", "App is not available.", Map.of());
            operation = findOperation(app, operationKey, trigger);
            if (operation == null) return failure(checks, "operation", "Operation", "This operation is not available for the selected app.", Map.of());
        } catch (IllegalArgumentException exception) {
            return failure(checks, "app", "App", "Choose a valid app before checking setup.", Map.of());
        }

        Map<String, Object> contract = contractFactory.create(appKey, operation, trigger);
        Connections_command connection = validateConnection(app, appKey, connectionId, userId, checks);
        if (connection != null) validateRequiredScopes(operation, connection, checks);
        validateRequiredFields(operation, config, checks);
        if (connection != null) validateResources(appKey, operation, connection.getId(), userId, config, checks);
        Map<String, Object> dataIn = validateInputPreview(input, config, checks);

        boolean failed = checks.stream().anyMatch(check -> "FAIL".equals(check.status()));
        Map<String, Object> preview = sanitizationService.sanitize(Map.of(
                "input", input,
                "configuration", config,
                "dataIn", dataIn,
                "mode", "SETUP_CHECK"
        ));
        return new SetupValidationResult(!failed, List.copyOf(checks), preview,
                failed ? "Fix the failed checks before running this step live." : null, contract);
    }

    private Connections_command validateConnection(App app, String appKey, String connectionId,
                                                   UUID userId, List<SetupCheck> checks) {
        if (app.getAuthType() == AuthType.NONE) {
            checks.add(pass("connection", "Connection", "This step does not require an account connection."));
            return null;
        }
        if (connectionId == null || connectionId.isBlank() || "ADMIN_KEY".equalsIgnoreCase(connectionId)) {
            checks.add(fail("connection", "Connection", "Choose the exact account this step should use. Crescendo will not fall back to another account."));
            return null;
        }
        try {
            UUID id = UUID.fromString(connectionId);
            Connections_command connection = connectionRepository.findByIdAndUser_Id(id, userId).orElse(null);
            if (connection == null) {
                checks.add(fail("connection", "Connection", "The selected connection was not found or is not yours."));
                return null;
            }
            if (!appKey.equals(connection.getAppKey())) {
                checks.add(fail("connection", "Connection", "The selected connection belongs to " + connection.getAppKey() + "."));
                return null;
            }
            if (connection.getStatus() != ConnectionStatus.ACTIVE) {
                checks.add(fail("connection", "Connection", "Reconnect this account before testing the step."));
                return null;
            }
            tokenService.getValidCredentials(connection.getId(), userId);
            checks.add(pass("connection", "Connection", "Connected as " + connection.getName() + "."));
            return connection;
        } catch (IllegalArgumentException exception) {
            checks.add(fail("connection", "Connection", "The selected connection ID is invalid."));
        } catch (Exception exception) {
            checks.add(fail("connection", "Connection", "Crescendo could not verify this connection. Reconnect it and try again."));
        }
        return null;
    }

    private void validateRequiredFields(Map<String, Object> operation, Map<String, Object> config,
                                        List<SetupCheck> checks) {
        List<String> missing = new ArrayList<>();
        for (Map<String, Object> field : configSchema(operation)) {
            if (!Boolean.TRUE.equals(field.get("required"))) continue;
            Object value = config.get(String.valueOf(field.get("key")));
            if (value == null || (value instanceof String text && text.isBlank())
                    || (value instanceof List<?> list && list.isEmpty())) {
                missing.add(String.valueOf(field.getOrDefault("label", field.get("key"))));
            }
        }
        if (missing.isEmpty()) checks.add(pass("required-fields", "Required fields", "All required fields are present."));
        else checks.add(fail("required-fields", "Required fields", "Complete: " + String.join(", ", missing) + "."));
    }

    private void validateRequiredScopes(Map<String, Object> operation, Connections_command connection,
                                        List<SetupCheck> checks) {
        Object rawScopes = operation.get("requiredScopes");
        if (!(rawScopes instanceof List<?> required) || required.isEmpty()) return;
        if (connection.getGrantedScopes() == null || connection.getGrantedScopes().isBlank()) {
            checks.add(warn("scopes", "Permissions", "The provider did not report granted scopes. Crescendo will verify access while checking selected resources."));
            return;
        }
        java.util.Set<String> granted = java.util.Arrays.stream(connection.getGrantedScopes().split("[\\s,]+"))
                .filter(scope -> !scope.isBlank()).collect(java.util.stream.Collectors.toSet());
        List<String> missing = required.stream().map(String::valueOf).filter(scope -> !granted.contains(scope)).toList();
        if (missing.isEmpty()) checks.add(pass("scopes", "Permissions", "Required account permissions are present."));
        else checks.add(fail("scopes", "Permissions", "Reconnect this account and grant: " + String.join(", ", missing) + "."));
    }

    private void validateResources(String appKey, Map<String, Object> operation, UUID connectionId, UUID userId,
                                   Map<String, Object> config, List<SetupCheck> checks) {
        for (Map<String, Object> field : configSchema(operation)) {
            if (!"dynamic_dropdown".equals(String.valueOf(field.get("type")))) continue;
            String fieldKey = String.valueOf(field.get("key"));
            Object selected = config.get(fieldKey);
            String resourceType = String.valueOf(field.get("resourceType"));
            if (selected == null || String.valueOf(selected).isBlank() || resourceType.isBlank() || "null".equals(resourceType)) continue;
            if (String.valueOf(selected).contains("{{")) {
                checks.add(warn("resource-" + fieldKey, String.valueOf(field.getOrDefault("label", fieldKey)),
                        "This value comes from an earlier step and will be checked when real input is available."));
                continue;
            }
            try {
                List<ResourceOption> options = resourceFetchService.fetchResources(appKey, resourceType, connectionId,
                        userId, dependencyParams(field, config));
                boolean found = options.stream().anyMatch(option -> String.valueOf(selected).equals(option.id())
                        || String.valueOf(selected).equalsIgnoreCase(option.label()));
                if (found) {
                    checks.add(pass("resource-" + fieldKey, String.valueOf(field.getOrDefault("label", fieldKey)), "Selected resource is accessible."));
                } else {
                    checks.add(fail("resource-" + fieldKey, String.valueOf(field.getOrDefault("label", fieldKey)), "Selected resource is unavailable in this account."));
                }
            } catch (Exception exception) {
                checks.add(warn("resource-" + fieldKey, String.valueOf(field.getOrDefault("label", fieldKey)),
                        "Crescendo could not verify this resource now. Check the account permissions and try again."));
            }
        }
    }

    private Map<String, Object> validateInputPreview(Map<String, Object> input, Map<String, Object> config, List<SetupCheck> checks) {
        Map<String, Object> dataIn = expressionResolver.resolveForTest(config, input);

        List<String> unresolved = new ArrayList<>();
        collectUnresolvedVariables(dataIn, unresolved);

        boolean hasExpressions = config.values().stream().anyMatch(this::containsExpression);

        if (!unresolved.isEmpty()) {
            checks.add(warn("input-preview", "Data in preview",
                    "Unresolved variables: " + String.join(", ", unresolved) + ". Provide sample data to preview these values."));
        } else if (hasExpressions) {
            checks.add(pass("input-preview", "Data in preview", "All mapped variables resolved successfully from sample input."));
        } else {
            checks.add(pass("input-preview", "Data in preview", "Configuration is static and ready to be used."));
        }
        return dataIn;
    }

    private boolean containsExpression(Object val) {
        if (val instanceof String s) return s.contains("{{");
        if (val instanceof Map<?, ?> m) return m.values().stream().anyMatch(this::containsExpression);
        if (val instanceof List<?> l) return l.stream().anyMatch(this::containsExpression);
        return false;
    }

    private void collectUnresolvedVariables(Object val, List<String> unresolved) {
        if (val instanceof String s) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}").matcher(s);
            while (m.find()) {
                String token = m.group(0);
                if (!unresolved.contains(token)) unresolved.add(token);
            }
        } else if (val instanceof Map<?, ?> m) {
            m.values().forEach(v -> collectUnresolvedVariables(v, unresolved));
        } else if (val instanceof List<?> l) {
            l.forEach(v -> collectUnresolvedVariables(v, unresolved));
        }
    }

    private SetupValidationResult failure(List<SetupCheck> checks, String id, String label, String detail,
                                          Map<String, Object> contract) {
        checks.add(fail(id, label, detail));
        return new SetupValidationResult(false, List.copyOf(checks), Map.of(), detail, contract);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> configSchema(Map<String, Object> operation) {
        if (!(operation.get("configSchema") instanceof List<?> raw)) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object field : raw) if (field instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            map.forEach((key, value) -> converted.put(String.valueOf(key), value));
            result.add(converted);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findOperation(App app, String key, boolean trigger) {
        List<Map<String, Object>> operations = trigger ? app.getTriggers() : app.getActions();
        String keyField = trigger ? "triggerKey" : "actionKey";
        if (operations == null) return null;
        return operations.stream().filter(operation -> key.equals(String.valueOf(operation.get(keyField))))
                .findFirst().map(LinkedHashMap::new).orElse(null);
    }

    private Map<String, String> dependencyParams(Map<String, Object> field, Map<String, Object> config) {
        Map<String, String> params = new LinkedHashMap<>();
        Object dependsOn = field.get("dependsOn");
        if (dependsOn instanceof String key && config.get(key) != null) params.put(key, String.valueOf(config.get(key)));
        if (dependsOn instanceof List<?> keys) for (Object key : keys) {
            Object value = config.get(String.valueOf(key));
            if (value != null) params.put(String.valueOf(key), String.valueOf(value));
        }
        return params;
    }

    private SetupCheck pass(String id, String label, String detail) { return new SetupCheck(id, label, "PASS", detail); }
    private SetupCheck warn(String id, String label, String detail) { return new SetupCheck(id, label, "WARN", detail); }
    private SetupCheck fail(String id, String label, String detail) { return new SetupCheck(id, label, "FAIL", detail); }
}
