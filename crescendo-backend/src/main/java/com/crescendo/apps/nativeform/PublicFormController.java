package com.crescendo.apps.nativeform;

import com.crescendo.enums.StepType;
import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.workflow_run.WorkflowRunService;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_command;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/public/forms")
public class PublicFormController {

    private final Steps_commandRepository stepsRepo;
    private final WorkflowRunService workflowRunService;

    public PublicFormController(Steps_commandRepository stepsRepo,
                                WorkflowRunService workflowRunService) {
        this.stepsRepo = stepsRepo;
        this.workflowRunService = workflowRunService;
    }

    @GetMapping("/{formKey}")
    @Transactional(readOnly = true)
    public Map<String, Object> getForm(@PathVariable String formKey) {
        Steps_command trigger = findFormTrigger(formKey);
        Map<String, Object> config = trigger.getConfiguration() != null ? trigger.getConfiguration() : Map.of();
        return Map.of(
                "formKey", formKey,
                "title", config.getOrDefault("title", "Crescendo Form"),
                "fields", config.getOrDefault("fields", java.util.List.of()),
                "successMessage", config.getOrDefault("successMessage", "Thanks, your form was submitted")
        );
    }

    @PostMapping("/{formKey}")
    @Transactional
    public Map<String, Object> submitForm(@PathVariable String formKey,
                                          @RequestBody(required = false) Map<String, Object> body) {
        Steps_command trigger = findFormTrigger(formKey);
        Workflow_command workflow = trigger.getWorkflow();
        if (workflow.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Public native forms require an account-owned workflow");
        }

        Map<String, Object> triggerData = new HashMap<>();
        triggerData.put("source", "native-form");
        triggerData.put("formKey", formKey);
        triggerData.put("submittedAt", Instant.now().toString());
        triggerData.put("data", body != null ? body : Map.of());

        LogbookDto.WorkflowRunSummaryResponse run = workflowRunService.startRun(
                workflow.getUser().getId(),
                workflow.getId(),
                new LogbookDto.StartWorkflowRunRequest(triggerData)
        );

        return Map.of(
                "accepted", true,
                "workflowRunId", run.id(),
                "status", run.status()
        );
    }

    private Steps_command findFormTrigger(String formKey) {
        if (formKey == null || formKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found");
        }

        return stepsRepo.findAll().stream()
                .filter(step -> step.getDeletedAt() == null)
                .filter(step -> step.getType() == StepType.TRIGGER)
                .filter(step -> "native-form".equals(step.getAppKey()))
                .filter(step -> "form-submit".equals(step.getActionKey()))
                .filter(step -> {
                    Workflow_command workflow = step.getWorkflow();
                    return workflow != null && workflow.getDeletedAt() == null && workflow.isActive();
                })
                .filter(step -> formKey.equals(String.valueOf(
                        (step.getConfiguration() != null ? step.getConfiguration() : Map.of()).get("formKey"))))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found"));
    }
}
