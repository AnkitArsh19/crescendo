package com.crescendo.workflow;

import com.crescendo.security.AuthenticatedUser;
import com.crescendo.shared.infrastructure.sse.WorkflowSseService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(Authentication authentication) {
        SseEmitter emitter = sseService.connect(AuthenticatedUser.userId(authentication));
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }
}
