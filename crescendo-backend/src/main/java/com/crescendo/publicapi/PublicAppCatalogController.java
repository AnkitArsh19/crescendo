package com.crescendo.publicapi;

import com.crescendo.app.AppDto;
import com.crescendo.app.AppService;
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
public class PublicAppCatalogController {
    private final AppService appService;

    public PublicAppCatalogController(AppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public ResponseEntity<List<AppDto.AppSummaryResponse>> listApps(Authentication auth) {
        require(auth, APP_READ);
        return ResponseEntity.ok(appService.listApps());
    }

    @GetMapping("/{appKey}")
    public ResponseEntity<AppDto.AppDetailResponse> getApp(@PathVariable String appKey, Authentication auth) {
        require(auth, APP_READ);
        return ResponseEntity.ok(appService.getApp(appKey));
    }
}
