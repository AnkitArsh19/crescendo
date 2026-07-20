package com.crescendo.workflow;

import com.crescendo.security.AuthenticatedUser;
import com.crescendo.shared.infrastructure.sse.WorkflowSseService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Authenticated, per-user workflow cache invalidation stream. */
@RestController
@RequestMapping("/workflows/events")
public class WorkflowSseController {

    private final WorkflowSseService sseService;

    public WorkflowSseController(WorkflowSseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping(produces = "text/event-stream")
    public SseEmitter stream(Authentication authentication) {
        return sseService.connect(AuthenticatedUser.userId(authentication));
    }
}
