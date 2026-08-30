package com.crescendo.app;

import com.crescendo.admin.PlatformKeyRepository;
import com.crescendo.execution.action.ActionHandlerRegistry;
import com.crescendo.execution.test.OperationTestContractFactory;
import com.crescendo.shared.domain.valueobject.AppKey;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-only service for the app catalog.
 * App definitions are seeded via database migrations — this service
 * only exposes cached read endpoints.
 *
 * <p>Internal apps (e.g. debug log) are filtered out of the listing
 * so they never appear in the user-facing app catalog.
 *
 * <p>{@code hasPlatformKey} is populated dynamically by querying the
 * {@code platform_key} table for enabled platform keys. This lets the frontend
 * show a "Use Crescendo's Key" toggle for apps where the admin has pre-configured
 * credentials, without the user needing to connect their own account.
 *
 * <p><strong>Credential Source Security:</strong> Credentials stored in this
 * system are encrypted at rest with server-managed AES-256-GCM keys. The server
 * must decrypt them to execute actions on behalf of the user — this is by design
 * for a workflow automation platform and is distinct from end-to-end encryption.
 */
@Service
public class AppService {

    private final AppRepository appRepo;
    private final ActionHandlerRegistry actionHandlerRegistry;
    private final PlatformKeyRepository platformKeyRepo;
    private final OperationTestContractFactory testContractFactory;

    @org.springframework.beans.factory.annotation.Value("${gemini.api.key:}")
    private String geminiApiKey;

    @org.springframework.beans.factory.annotation.Value("${sarvam.api.key:}")
    private String sarvamApiKey;

    @org.springframework.beans.factory.annotation.Value("${telegram.bot.token:}")
    private String telegramBotToken;

    public AppService(AppRepository appRepo, 
                      ActionHandlerRegistry actionHandlerRegistry, 
                      PlatformKeyRepository platformKeyRepo,
                      OperationTestContractFactory testContractFactory) {
        this.appRepo = appRepo;
        this.actionHandlerRegistry = actionHandlerRegistry;
        this.platformKeyRepo = platformKeyRepo;
        this.testContractFactory = testContractFactory;
    }

    @Cacheable(value = "apps", key = "'all:v3'")
    public List<AppDto.AppSummaryResponse> listApps() {
        Set<String> platformKeyAppKeys = loadEnabledPlatformKeyAppKeys();
        return appRepo.findAll()
                .stream()
                .filter(app -> !app.isInternal())
                .map(app -> toSummary(app, platformKeyAppKeys))
                .toList();
    }

    @Cacheable(value = "apps", key = "'detail:v3:' + #appKey")
    public AppDto.AppDetailResponse getApp(String appKey) {
        App app = appRepo.findById(AppKey.of(appKey))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App not found"));
        Set<String> platformKeyAppKeys = loadEnabledPlatformKeyAppKeys();
        return toDetail(app, platformKeyAppKeys);
    }

    /**
     * Loads the set of appKeys that have an enabled platform key in the database
     * or configured directly in application.properties.
     * Used for O(1) lookup when mapping the app list.
     */
    private Set<String> loadEnabledPlatformKeyAppKeys() {
        Set<String> keys = platformKeyRepo.findAllByEnabledTrue()
                .stream()
                .map(pk -> pk.getAppKey())
                .collect(Collectors.toSet());

        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            keys.add("gemini");
        }
        if (sarvamApiKey != null && !sarvamApiKey.isBlank()) {
            keys.add("sarvam");
        }
        if (telegramBotToken != null && !telegramBotToken.isBlank()) {
            keys.add("telegram");
        }
        return keys;
    }

    private AppDto.AppSummaryResponse toSummary(App app, Set<String> platformKeyAppKeys) {
        int triggerCount = app.getTriggers() != null ? app.getTriggers().size() : 0;
        int actionCount = app.getActions() != null ? app.getActions().size() : 0;
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
                app.isInternal(),
                platformKeyAppKeys.contains(app.getAppKey()),
                triggerCount > 0,
                actionCount > 0,
                triggerCount,
                actionCount
        );
    }

    private AppDto.AppDetailResponse toDetail(App app, Set<String> platformKeyAppKeys) {
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
                withTestContracts(app.getAppKey(), app.getTriggers(), true),
                withTestContracts(app.getAppKey(), runnableActions, false),
                app.getCredentialSchema(),
                app.getCategory(),
                app.getHelpUrl(),
                app.isInternal(),
                platformKeyAppKeys.contains(app.getAppKey())
        );
    }

    private List<Map<String, Object>> withTestContracts(String appKey,
                                                         List<Map<String, Object>> operations,
                                                         boolean trigger) {
        if (operations == null) return List.of();
        return operations.stream().map(operation -> {
            Map<String, Object> enriched = new java.util.LinkedHashMap<>(operation);
            enriched.put("testContract", testContractFactory.create(appKey, operation, trigger));
            return enriched;
        }).toList();
    }
}
