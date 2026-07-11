package com.crescendo.publicapi;

import com.crescendo.app.AppDto;
import com.crescendo.app.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.crescendo.publicapi.PublicApiScopes.APP_READ;
import static com.crescendo.publicapi.PublicApiScopes.require;

@RestController
@RequestMapping("/api/v1/apps")
@Tag(name = "App Catalog", description = "Public API for retrieving supported apps and actions")
public class PublicAppCatalogController {
    private final AppService appService;

    public PublicAppCatalogController(AppService appService) {
        this.appService = appService;
    }

    @GetMapping
    @Operation(summary = "List apps", description = "Lists all supported apps in the catalog. Requires app:read scope.")
    public ResponseEntity<List<AppDto.AppSummaryResponse>> listApps(Authentication auth) {
        require(auth, APP_READ);
        return ResponseEntity.ok(appService.listApps());
    }

    @GetMapping("/{appKey}")
    @Operation(summary = "Get app details", description = "Gets triggers, actions, and connection schemas for a specific app. Requires app:read scope.")
    public ResponseEntity<AppDto.AppDetailResponse> getApp(@PathVariable String appKey, Authentication auth) {
        require(auth, APP_READ);
        return ResponseEntity.ok(appService.getApp(appKey));
    }
}
