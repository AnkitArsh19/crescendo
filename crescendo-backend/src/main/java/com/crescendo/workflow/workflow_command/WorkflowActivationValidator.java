package com.crescendo.workflow.workflow_command;

import com.crescendo.app.AppDto;
import com.crescendo.app.AppService;
import com.crescendo.enums.StepType;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class WorkflowActivationValidator {

    private final Steps_commandRepository stepsRepo;
    private final AppService appService;

    public WorkflowActivationValidator(Steps_commandRepository stepsRepo, AppService appService) {
        this.stepsRepo = stepsRepo;
        this.appService = appService;
    }

    public void validateForActivation(UUID workflowId) {
        List<Steps_command> steps = stepsRepo.findActiveByWorkflowIdOrdered(workflowId);

        if (steps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot activate a workflow with no steps. Add at least a trigger and one action.");
        }
        if (steps.getFirst().getType() != StepType.TRIGGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The first step must be a trigger.");
        }
        long triggerCount = steps.stream().filter(s -> s.getType() == StepType.TRIGGER).count();
        if (triggerCount > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A workflow can only have one trigger. Remove extra triggers before activating.");
        }
        if (steps.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A workflow needs at least a trigger and one action step.");
        }

        for (int i = 0; i < steps.size(); i++) {
            validateStep(steps.get(i), i);
        }
    }

    private void validateStep(Steps_command step, int index) {
        String appKey = step.getAppKey();
        if (appKey == null || appKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Step %d: select an app.", index + 1));
        }

        AppDto.AppDetailResponse appDef;
        try {
            appDef = appService.getApp(appKey);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Step %d: Invalid appKey: %s", index + 1, appKey));
        }

        boolean needsAuth = !"NONE".equals(appDef.authType());
        if (needsAuth && step.getConnectionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Step %d: select a connected account.", index + 1));
        }

        String opKey = step.getActionKey();
        if (opKey == null || opKey.isBlank()) {
            String opType = step.getType() == StepType.TRIGGER ? "trigger event" : "action";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Step %d: select a %s.", index + 1, opType));
        }

        List<Map<String, Object>> defs = step.getType() == StepType.TRIGGER ? appDef.triggers() : appDef.actions();
        Map<String, Object> def = defs.stream()
                .filter(d -> opKey.equals(d.get(step.getType() == StepType.TRIGGER ? "triggerKey" : "actionKey")))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Step %d: Invalid operation key for app: %s", index + 1, opKey)));

        Object schemaObj = def.get("configSchema");
        validateConfiguration(step.getConfiguration(), schemaObj, index);
    }

    @SuppressWarnings("unchecked")
    private void validateConfiguration(Map<String, Object> config, Object schemaObj, int index) {
        if (schemaObj == null) return;
        Map<String, Object> safeConfig = config != null ? config : Map.of();

        if (schemaObj instanceof List<?> list) {
            // New structured format
            for (Object item : list) {
                if (item instanceof Map<?, ?> field) {
                    Boolean required = (Boolean) field.get("required");
                    if (Boolean.TRUE.equals(required)) {
                        String key = (String) field.get("key");
                        String label = (String) field.get("label");
                        if (label == null) label = key;

                        Object value = safeConfig.get(key);
                        if (value == null || value.toString().trim().isEmpty()) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    String.format("Step %d: '%s' is required.", index + 1, label));
                        }
                    }
                }
            }
        } else if (schemaObj instanceof Map<?, ?> map) {
            // Legacy format
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = (String) entry.getKey();
                String hint = String.valueOf(entry.getValue());
                if (hint != null && hint.toLowerCase().contains("required")) {
                    Object value = safeConfig.get(key);
                    if (value == null || value.toString().trim().isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                String.format("Step %d: '%s' is required.", index + 1, key));
                    }
                }
            }
        }
    }
}
