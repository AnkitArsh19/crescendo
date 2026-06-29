package com.crescendo.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/catalog")
public class InternalCatalogController {

    private final AppService appService;
    private final String catalogVersion;
    private List<Map<String, Object>> minifiedCatalogCache;

    public InternalCatalogController(AppService appService) {
        this.appService = appService;
        // Generate a random UUID on startup. This serves as our "hash".
        // Since the catalog is entirely static (based on compiled AppDefinition classes),
        // it only ever changes when the Java backend is redeployed/restarted!
        this.catalogVersion = UUID.randomUUID().toString();
    }

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        return Map.of("version", catalogVersion);
    }

    @GetMapping
    public Map<String, Object> getCatalog() {
        if (minifiedCatalogCache == null) {
            minifiedCatalogCache = appService.listApps().stream()
                .map(app -> {
                    AppDto.AppDetailResponse detail = appService.getApp(app.appKey());
                    return Map.<String, Object>of(
                        "appKey", app.appKey(),
                        "name", app.name(),
                        "triggers", detail.triggers().stream().map(t -> t.get("triggerKey")).collect(Collectors.toList()),
                        "actions", detail.actions().stream().map(a -> a.get("actionKey")).collect(Collectors.toList())
                    );
                })
                .collect(Collectors.toList());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("version", catalogVersion);
        response.put("catalog", minifiedCatalogCache);
        return response;
    }
}
