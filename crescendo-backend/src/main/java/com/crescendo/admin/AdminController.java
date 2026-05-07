package com.crescendo.admin;

import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import com.crescendo.enums.UserRole;
import com.crescendo.security.AppUserDetails;
import com.crescendo.security.access.AccessControlService;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Admin-only endpoints for user management, admin emails, and platform keys.
 *
 * Endpoints:
 *   GET    /admin/users                       — list all users
 *   POST   /admin/users/{userId}/promote      — promote a user to ADMIN
 *   POST   /admin/users/{userId}/demote       — demote an admin back to USER
 *   GET    /admin/emails                      — list whitelisted admin emails
 *   POST   /admin/emails                      — add an admin email
 *   DELETE /admin/emails/{email}              — remove an admin email
 *   GET    /admin/platform-keys               — list all platform keys
 *   POST   /admin/platform-keys               — create/update a platform key
 *   DELETE /admin/platform-keys/{appKey}      — remove a platform key
 *   PATCH  /admin/platform-keys/{appKey}/toggle — enable/disable a platform key
 *   GET    /admin/platform-keys/available     — public: list apps that have platform keys (for non-admin users)
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final User_commandRepository userRepo;
    private final AccessControlService accessControl;
    private final AdminEmailRepository adminEmailRepo;
    private final PlatformKeyRepository platformKeyRepo;
    private final ConnectionCredentialsCryptoService cryptoService;

    public AdminController(User_commandRepository userRepo,
                           AccessControlService accessControl,
                           AdminEmailRepository adminEmailRepo,
                           PlatformKeyRepository platformKeyRepo,
                           ConnectionCredentialsCryptoService cryptoService) {
        this.userRepo = userRepo;
        this.accessControl = accessControl;
        this.adminEmailRepo = adminEmailRepo;
        this.platformKeyRepo = platformKeyRepo;
        this.cryptoService = cryptoService;
    }

    // ── User Management ─────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listUsers(Authentication auth) {
        requireAdmin(auth);

        List<Map<String, Object>> users = userRepo.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId().toString(),
                        "email", u.getEmail() != null ? u.getEmail().value() : "",
                        "username", u.getUserName() != null ? u.getUserName() : "",
                        "role", u.getRole().name(),
                        "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
                ))
                .toList();

        return ResponseEntity.ok(users);
    }

    @PostMapping("/users/{userId}/promote")
    public ResponseEntity<Map<String, String>> promoteUser(
            @PathVariable UUID userId, Authentication auth) {
        requireAdmin(auth);

        User_command target = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (target.getRole() == UserRole.ADMIN) {
            return ResponseEntity.ok(Map.of("message", "User is already an admin"));
        }

        target.setRole(UserRole.ADMIN);
        userRepo.save(target);

        return ResponseEntity.ok(Map.of("message", "User promoted to admin"));
    }

    @PostMapping("/users/{userId}/demote")
    public ResponseEntity<Map<String, String>> demoteUser(
            @PathVariable UUID userId, Authentication auth) {
        AppUserDetails principal = extractPrincipal(auth);
        requireAdmin(auth);

        if (principal.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot demote yourself");
        }

        User_command target = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (target.getRole() != UserRole.ADMIN) {
            return ResponseEntity.ok(Map.of("message", "User is not an admin"));
        }

        target.setRole(UserRole.USER);
        userRepo.save(target);

        return ResponseEntity.ok(Map.of("message", "User demoted to regular user"));
    }

    // ── Admin Email Whitelist ───────────────────────────────────────────

    @GetMapping("/emails")
    public ResponseEntity<List<Map<String, Object>>> listAdminEmails(Authentication auth) {
        requireAdmin(auth);

        List<Map<String, Object>> emails = adminEmailRepo.findAll().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getId().toString());
                    m.put("email", e.getEmail());
                    m.put("addedBy", e.getAddedBy());
                    m.put("addedAt", e.getAddedAt() != null ? e.getAddedAt().toString() : "");
                    return m;
                })
                .toList();

        return ResponseEntity.ok(emails);
    }

    @PostMapping("/emails")
    @Transactional
    public ResponseEntity<Map<String, String>> addAdminEmail(
            @RequestBody Map<String, String> body, Authentication auth) {
        requireAdmin(auth);
        AppUserDetails principal = extractPrincipal(auth);

        String rawEmail = body.get("email");
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        final String email = rawEmail.trim().toLowerCase();

        if (adminEmailRepo.existsByEmail(email)) {
            return ResponseEntity.ok(Map.of("message", "Email already whitelisted"));
        }

        adminEmailRepo.save(new AdminEmail(email, principal.getUsername()));

        // If this user already exists, auto-promote them
        userRepo.findAll().stream()
                .filter(u -> u.getEmail() != null && email.equals(u.getEmail().value()))
                .findFirst()
                .ifPresent(user -> {
                    if (user.getRole() != UserRole.ADMIN) {
                        user.setRole(UserRole.ADMIN);
                        userRepo.save(user);
                    }
                });

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Admin email added"));
    }

    @DeleteMapping("/emails/{email}")
    @Transactional
    public ResponseEntity<Map<String, String>> removeAdminEmail(
            @PathVariable String email, Authentication auth) {
        requireAdmin(auth);
        email = email.trim().toLowerCase();

        if (!adminEmailRepo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not in whitelist");
        }

        adminEmailRepo.deleteByEmail(email);
        return ResponseEntity.ok(Map.of("message", "Admin email removed"));
    }

    // ── Platform Keys ──────────────────────────────────────────────────

    @GetMapping("/platform-keys")
    public ResponseEntity<List<Map<String, Object>>> listPlatformKeys(Authentication auth) {
        requireAdmin(auth);

        List<Map<String, Object>> keys = platformKeyRepo.findAll().stream()
                .map(k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", k.getId().toString());
                    m.put("appKey", k.getAppKey());
                    m.put("appName", k.getAppName());
                    m.put("enabled", k.isEnabled());
                    m.put("usageCount", k.getUsageCount());
                    m.put("addedBy", k.getAddedBy());
                    m.put("createdAt", k.getCreatedAt() != null ? k.getCreatedAt().toString() : "");
                    // Never expose credentials to the frontend
                    m.put("hasCredentials", k.getEncryptedCredentials() != null && !k.getEncryptedCredentials().isBlank());
                    return m;
                })
                .toList();

        return ResponseEntity.ok(keys);
    }

    @PostMapping("/platform-keys")
    @Transactional
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, String>> savePlatformKey(
            @RequestBody Map<String, Object> body, Authentication auth) {
        requireAdmin(auth);
        AppUserDetails principal = extractPrincipal(auth);

        String appKey = (String) body.get("appKey");
        String appName = (String) body.get("appName");
        Map<String, Object> credentials = (Map<String, Object>) body.get("credentials");

        if (appKey == null || appKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appKey is required");
        }
        if (credentials == null || credentials.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "credentials are required");
        }

        // Encrypt credentials
        Map<String, Object> sealed = cryptoService.seal(credentials);
        String encryptedJson;
        try {
            encryptedJson = new tools.jackson.databind.ObjectMapper().writeValueAsString(sealed);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize credentials");
        }

        PlatformKey pk = platformKeyRepo.findByAppKey(appKey)
                .orElse(new PlatformKey(appKey, appName, encryptedJson, principal.getUsername()));

        pk.setEncryptedCredentials(encryptedJson);
        pk.setEnabled(true);
        platformKeyRepo.save(pk);

        return ResponseEntity.ok(Map.of("message", "Platform key saved for " + appKey));
    }

    @DeleteMapping("/platform-keys/{appKey}")
    @Transactional
    public ResponseEntity<Map<String, String>> removePlatformKey(
            @PathVariable String appKey, Authentication auth) {
        requireAdmin(auth);

        PlatformKey pk = platformKeyRepo.findByAppKey(appKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Platform key not found"));

        platformKeyRepo.delete(pk);
        return ResponseEntity.ok(Map.of("message", "Platform key removed"));
    }

    @PatchMapping("/platform-keys/{appKey}/toggle")
    @Transactional
    public ResponseEntity<Map<String, Object>> togglePlatformKey(
            @PathVariable String appKey, Authentication auth) {
        requireAdmin(auth);

        PlatformKey pk = platformKeyRepo.findByAppKey(appKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Platform key not found"));

        pk.setEnabled(!pk.isEnabled());
        platformKeyRepo.save(pk);

        return ResponseEntity.ok(Map.of(
                "message", pk.isEnabled() ? "Platform key enabled" : "Platform key disabled",
                "enabled", pk.isEnabled()
        ));
    }

    /**
     * Public endpoint — returns list of appKeys that have an enabled platform key.
     * Non-admin users use this to know which apps have a platform key available.
     */
    @GetMapping("/platform-keys/available")
    public ResponseEntity<List<Map<String, Object>>> availablePlatformKeys() {
        List<Map<String, Object>> available = platformKeyRepo.findAllByEnabledTrue().stream()
                .map(k -> Map.<String, Object>of(
                        "appKey", k.getAppKey(),
                        "appName", k.getAppName() != null ? k.getAppName() : k.getAppKey()
                ))
                .toList();

        return ResponseEntity.ok(available);
    }

    // ── Helpers ──

    private void requireAdmin(Authentication auth) {
        if (!accessControl.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private AppUserDetails extractPrincipal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return details;
    }
}
