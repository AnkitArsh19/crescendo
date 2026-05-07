package com.crescendo.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only app catalog endpoints under /apps.
 *
 * The app catalog lists all available integrations and their
 * trigger/action definitions. These endpoints are authenticated
 * (user must be logged in) but not ownership-scoped.
 *
 *   GET /apps            — list all available apps (summary)
 *   GET /apps/{appKey}   — get app detail with triggers and actions
 */
@RestController
@RequestMapping("/apps")
public class AppController {

    private final AppService appService;

    public AppController(AppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public ResponseEntity<List<AppDto.AppSummaryResponse>> listApps() {
        return ResponseEntity.ok(appService.listApps());
    }

    @GetMapping("/{appKey}")
    public ResponseEntity<AppDto.AppDetailResponse> getApp(@PathVariable String appKey) {
        return ResponseEntity.ok(appService.getApp(appKey));
    }
}
