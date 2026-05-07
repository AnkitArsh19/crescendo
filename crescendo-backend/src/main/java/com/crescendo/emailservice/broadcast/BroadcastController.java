package com.crescendo.emailservice.broadcast;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/settings/broadcasts")
public class BroadcastController {

    private final BroadcastService broadcastService;

    public BroadcastController(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @PostMapping
    public ResponseEntity<BroadcastDto.BroadcastResponse> create(
            @Valid @RequestBody BroadcastDto.CreateBroadcastRequest req,
            Authentication auth) {
        Broadcast b = broadcastService.create(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(b));
    }

    @GetMapping
    public ResponseEntity<List<BroadcastDto.BroadcastResponse>> list(Authentication auth) {
        List<BroadcastDto.BroadcastResponse> result = broadcastService.list(userId(auth))
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{broadcastId}")
    public ResponseEntity<BroadcastDto.BroadcastResponse> get(@PathVariable UUID broadcastId,
                                                               Authentication auth) {
        return ResponseEntity.ok(toResponse(broadcastService.get(userId(auth), broadcastId)));
    }

    @PostMapping("/{broadcastId}/send")
    public ResponseEntity<BroadcastDto.BroadcastResponse> send(@PathVariable UUID broadcastId,
                                                                Authentication auth) {
        Broadcast b = broadcastService.send(userId(auth), broadcastId);
        return ResponseEntity.ok(toResponse(b));
    }

    @DeleteMapping("/{broadcastId}")
    public ResponseEntity<Void> delete(@PathVariable UUID broadcastId, Authentication auth) {
        broadcastService.delete(userId(auth), broadcastId);
        return ResponseEntity.noContent().build();
    }

    private BroadcastDto.BroadcastResponse toResponse(Broadcast b) {
        return new BroadcastDto.BroadcastResponse(
                b.getId(), b.getTemplateId(), b.getFromAddress(), b.getStatus(),
                b.getTotalCount(), b.getSentCount(), b.getFailedCount(), b.getError(),
                b.getCreatedAt(), b.getStartedAt(), b.getCompletedAt());
    }

}
