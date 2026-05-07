package com.crescendo.app;

import com.crescendo.execution.action.ActionHandlerRegistry;
import com.crescendo.shared.domain.valueobject.AppKey;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Read-only service for the app catalog.
 * App definitions are seeded via database migrations — this service
 * only exposes cached read endpoints.
 *
 * <p>Internal apps (e.g. debug log) are filtered out of the listing
 * so they never appear in the user-facing app catalog.
 */
@Service
public class AppService {

    private final AppRepository appRepo;
    private final ActionHandlerRegistry actionHandlerRegistry;

    public AppService(AppRepository appRepo, ActionHandlerRegistry actionHandlerRegistry) {
        this.appRepo = appRepo;
        this.actionHandlerRegistry = actionHandlerRegistry;
    }

    @Cacheable(value = "apps", key = "'all:v2'")
    public List<AppDto.AppSummaryResponse> listApps() {
        return appRepo.findAll()
                .stream()
                .filter(app -> !app.isInternal())
                .map(this::toSummary)
                .toList();
    }

    @Cacheable(value = "apps", key = "'detail:v2:' + #appKey")
    public AppDto.AppDetailResponse getApp(String appKey) {
        App app = appRepo.findById(AppKey.of(appKey))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App not found"));
        return toDetail(app);
    }

    private AppDto.AppSummaryResponse toSummary(App app) {
        return new AppDto.AppSummaryResponse(
                app.getAppKey(),
                app.getName(),
                app.getDescription(),
                app.getLogoUrl(),
                app.getAuthType().name(),
                app.getAltAuthType() != null ? app.getAltAuthType().name() : null,
                app.getCredentialSchema(),
                app.getCategory(),
                app.getHelpUrl(),
                app.isInternal()
        );
    }

    private AppDto.AppDetailResponse toDetail(App app) {
        List<Map<String, Object>> runnableActions = app.getActions().stream()
            .filter(a -> {
                Object actionKey = a.get("actionKey");
                return actionKey != null
                    && actionHandlerRegistry.hasHandler(app.getAppKey(), actionKey.toString());
            })
            .toList();

        return new AppDto.AppDetailResponse(
                app.getAppKey(),
                app.getName(),
                app.getDescription(),
                app.getLogoUrl(),
                app.getAuthType().name(),
                app.getAltAuthType() != null ? app.getAltAuthType().name() : null,
                app.getTriggers(),
            runnableActions,
                app.getCredentialSchema(),
                app.getCategory(),
                app.getHelpUrl(),
                app.isInternal()
        );
    }
}
