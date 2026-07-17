package com.crescendo.auth.controller;

import com.crescendo.auth.dto.SessionDto;
import com.crescendo.security.AppUserDetails;
import com.crescendo.security.JWTService;
import com.crescendo.security.alerts.GeoIpService;
import com.crescendo.user.user_command.user_session.UserSession;
import com.crescendo.user.user_command.user_session.UserSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth/sessions")
public class SessionController {

    private final UserSessionRepository sessionRepo;
    private final JWTService jwtService;
    private final GeoIpService geoIpService;

    public SessionController(UserSessionRepository sessionRepo, JWTService jwtService, GeoIpService geoIpService) {
        this.sessionRepo = sessionRepo;
        this.jwtService = jwtService;
        this.geoIpService = geoIpService;
    }

    @GetMapping
    public List<SessionDto> getActiveSessions(
            @AuthenticationPrincipal AppUserDetails user,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String currentToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            currentToken = authHeader.substring(7);
        }
        String currentSidStr = currentToken != null ? jwtService.extractSessionId(currentToken) : null;
        UUID currentSid = currentSidStr != null ? UUID.fromString(currentSidStr) : null;

        List<UserSession> activeSessions = sessionRepo.findAllActiveByUserId(user.getId(), Instant.now());

        return activeSessions.stream().map(s -> {
            String ip = s.getClientIp() != null ? s.getClientIp().value() : null;
            String country = geoIpService.lookupCountry(ip).orElse(null);
            boolean isCurrent = s.getId().equals(currentSid);

            return new SessionDto(
                    s.getId(),
                    s.getDeviceLabel(),
                    ip,
                    country,
                    s.getCreatedAt(),
                    s.getLastUsedAt(),
                    isCurrent
            );
        }).collect(Collectors.toList());
    }

    @DeleteMapping("/{sessionId}")
    @Transactional
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable UUID sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        UserSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your session");
        }

        session.setRevokedAt(Instant.now());

        String currentToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            currentToken = authHeader.substring(7);
        }
        String currentSidStr = currentToken != null ? jwtService.extractSessionId(currentToken) : null;
        
        if (currentSidStr != null && currentSidStr.equals(sessionId.toString())) {
            // Revoking current session. It will be effectively dead for refresh.
            // The access token remains valid for max 15 minutes.
        }

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> revokeAllSessions(@AuthenticationPrincipal AppUserDetails user) {
        List<UserSession> activeSessions = sessionRepo.findAllActiveByUserId(user.getId(), Instant.now());
        activeSessions.forEach(s -> s.setRevokedAt(Instant.now()));
        return ResponseEntity.noContent().build();
    }

    public record RevokeTargetDto(String deviceLabel, Instant createdAt) {}

    /**
     * Validates the token and returns what session it targets so the frontend can render the confirmation.
     */
    @GetMapping(value = "/revoke-confirm", produces = "application/json")
    public ResponseEntity<?> revokeConfirmPage(@RequestParam("token") String token) {
        try {
            JWTService.SessionRevokeClaims claims = jwtService.parseSessionRevokeToken(token);
            if (claims.sessionId() != null) {
                return sessionRepo.findById(claims.sessionId())
                        .<ResponseEntity<?>>map(session -> ResponseEntity.ok(new RevokeTargetDto(session.getDeviceLabel(), session.getCreatedAt())))
                        .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).body("This session was already revoked."));
            } else {
                return ResponseEntity.ok(new RevokeTargetDto("All your devices", Instant.now()));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid or expired link.");
        }
    }

    /**
     * Processes the actual revocation from the frontend.
     */
    @PostMapping(value = "/revoke-by-token", produces = "application/json")
    @Transactional
    public ResponseEntity<?> revokeByToken(@RequestParam("token") String token) {
        try {
            JWTService.SessionRevokeClaims claims = jwtService.parseSessionRevokeToken(token);
            
            if (claims.sessionId() != null) {
                // Revoke specific session
                sessionRepo.findById(claims.sessionId()).ifPresent(session -> {
                    if (session.getUser().getId().equals(claims.userId())) {
                        session.setRevokedAt(Instant.now());
                    }
                });
            } else {
                // Revoke all sessions for user
                List<UserSession> activeSessions = sessionRepo.findAllActiveByUserId(claims.userId(), Instant.now());
                activeSessions.forEach(s -> s.setRevokedAt(Instant.now()));
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid or expired link.");
        }
    }
}
