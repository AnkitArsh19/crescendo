package com.crescendo.emailservice.customevent;

import com.crescendo.execution.suspension.WorkflowSuspensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static com.crescendo.security.AuthenticatedUser.userId;
import static com.crescendo.publicapi.PublicApiScopes.CUSTOM_EVENT_READ;
import static com.crescendo.publicapi.PublicApiScopes.CUSTOM_EVENT_WRITE;
import static com.crescendo.publicapi.PublicApiScopes.require;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/email/custom-events")
@Transactional
@Tag(name = "Custom Events", description = "Public API for managing custom events that trigger or resume workflows")
public class CustomEventController {

    private final CustomEventRepository repository;
    private final WorkflowSuspensionService suspensionService;

    public CustomEventController(CustomEventRepository repository, WorkflowSuspensionService suspensionService) {
        this.repository = repository;
        this.suspensionService = suspensionService;
    }

    public record CreateCustomEventRequest(String name, Map<String, Object> jsonSchema) {}
    public record FireCustomEventRequest(String correlationKey, Map<String, Object> payload) {}

    @PostMapping
    @Operation(summary = "Create a custom event", operationId = "createEvent")
    public CustomEvent createEvent(Authentication auth,
                                   @RequestBody CreateCustomEventRequest req) {
        require(auth, CUSTOM_EVENT_WRITE);
        UUID userId = userId(auth);
        if (repository.findByUserIdAndName(userId, req.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event name already exists");
        }
        CustomEvent event = new CustomEvent(UUID.randomUUID(), userId, req.name(), req.jsonSchema());
        return repository.save(event);
    }

    @GetMapping
    @Operation(summary = "List custom events", operationId = "listEvents")
    public List<CustomEvent> listEvents(Authentication auth) {
        require(auth, CUSTOM_EVENT_READ);
        UUID userId = userId(auth);
        return repository.findByUserId(userId);
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Delete a custom event", operationId = "deleteEvent")
    public void deleteEvent(Authentication auth, @PathVariable String name) {
        require(auth, CUSTOM_EVENT_WRITE);
        UUID userId = userId(auth);
        repository.deleteByUserIdAndName(userId, name);
    }

    @PostMapping("/{name}/fire")
    @Operation(summary = "Fire a custom event", operationId = "fireEvent")
    public void fireEvent(Authentication auth,
                          @PathVariable String name,
                          @RequestBody FireCustomEventRequest req) {
        require(auth, CUSTOM_EVENT_WRITE);
        UUID userId = userId(auth);
        // Validate event exists
        repository.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        // When a user fires a custom event, we try to resume any workflow waiting for it.
        // The suspension correlation key includes user ID and event name to prevent cross-tenant issues.
        String fullKey = "custom:" + userId + ":" + name + ":" + req.correlationKey();
        suspensionService.resume(fullKey, req.payload());
        // If no suspension found, the event may be a trigger-start — handled via the webhook/trigger system.
    }
}
