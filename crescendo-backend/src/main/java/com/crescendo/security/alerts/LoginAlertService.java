package com.crescendo.security.alerts;

import com.crescendo.auth.domain_event.UserSessionCreatedEvent;
import com.crescendo.security.JWTService;
import com.crescendo.emailservice.NotificationService;
import com.crescendo.user.user_command.user_session.UserSession;
import com.crescendo.user.user_command.user_session.UserSessionRepository;
import com.crescendo.user.user_query.User_queryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LoginAlertService {
    private static final Logger log = LoggerFactory.getLogger(LoginAlertService.class);

    private final UserSessionRepository sessionRepo;
    private final User_queryRepository userQueryRepo;
    private final NotificationService notificationService;
    private final GeoIpService geoIpService;
    private final JWTService jwtService;
    private final String frontendUrl;

    public LoginAlertService(
            UserSessionRepository sessionRepo,
            User_queryRepository userQueryRepo,
            NotificationService notificationService,
            GeoIpService geoIpService,
            JWTService jwtService,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.sessionRepo = sessionRepo;
        this.userQueryRepo = userQueryRepo;
        this.notificationService = notificationService;
        this.geoIpService = geoIpService;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Evaluates new sessions to determine if a security alert should be sent.
     * Uses TRUE INDEPENDENT OR LOGIC: triggers if the device is unseen OR the country is unseen.
     */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSessionCreated(UserSessionCreatedEvent event) {
        userQueryRepo.findById(event.aggregateId()).ifPresent(user -> {
            // Wait, we need the session's deviceId to compare. The event has deviceLabel but not deviceId?
            // Let's fetch the session directly from DB to get full metadata.
            sessionRepo.findById(event.getSessionId()).ifPresent(currentSession -> {
                evaluateSessionAndAlert(user.getEmailId(), currentSession);
            });
        });
    }

    private void evaluateSessionAndAlert(String email, UserSession current) {
        // Find recent history (up to 10 past sessions, excluding the current one)
        List<UserSession> history = sessionRepo.findTop10ByUser_IdOrderByCreatedAtDesc(current.getUser().getId())
                .stream()
                .filter(s -> !s.getId().equals(current.getId()))
                .toList();

        // If history is empty, it's their very first login. We can skip the alert or send a welcome alert.
        // Usually, a "new sign-in" alert isn't needed for the very first signup login.
        if (history.isEmpty()) {
            return;
        }

        String currentDeviceId = current.getDeviceId() != null ? current.getDeviceId().value() : null;
        String currentIp = current.getClientIp() != null ? current.getClientIp().value() : null;
        String currentCountry = geoIpService.lookupCountry(currentIp).orElse(null);

        boolean isNewDevice = true;
        boolean isNewCountry = true;

        for (UserSession past : history) {
            String pastDeviceId = past.getDeviceId() != null ? past.getDeviceId().value() : null;
            String pastIp = past.getClientIp() != null ? past.getClientIp().value() : null;
            String pastCountry = geoIpService.lookupCountry(pastIp).orElse(null);

            if (currentDeviceId != null && currentDeviceId.equals(pastDeviceId)) {
                isNewDevice = false;
            }
            if (currentCountry != null && currentCountry.equals(pastCountry)) {
                isNewCountry = false;
            }
        }

        // Only alert if we have actual signal data (don't alert just because we failed to get a country)
        boolean alertForDevice = (currentDeviceId != null && isNewDevice);
        boolean alertForCountry = (currentCountry != null && isNewCountry);

        if (alertForDevice || alertForCountry) {
            log.info("Triggering Smart Login Alert for userId={} (newDevice={}, newCountry={})",
                    current.getUser().getId(), alertForDevice, alertForCountry);

            String revokeToken = jwtService.issueSessionRevokeToken(current.getUser().getId(), current.getId());
            String revokeUrl = frontendUrl + "/auth/revoke-session?token=" + revokeToken;

            String device = current.getDeviceLabel() != null ? current.getDeviceLabel() : "Unknown Device";
            String location = currentIp != null ? "IP: " + currentIp : "Unknown Location";
            if (currentCountry != null) {
                location += " (" + currentCountry + ")";
            }

            notificationService.sendSmartLoginAlertEmail(email, device, location, currentCountry, revokeUrl);
        }
    }

    @TransactionalEventListener
    public void onSuspiciousSessionIp(com.crescendo.auth.domain_event.SuspiciousSessionIpEvent event) {
        userQueryRepo.findById(event.aggregateId()).ifPresent(user -> {
            String revokeToken = jwtService.issueSessionRevokeToken(user.getId(), event.getSessionId());
            String revokeUrl = frontendUrl + "/auth/revoke-session?token=" + revokeToken;
            notificationService.sendSuspiciousActivityEmail(user.getEmailId(), event.getOriginalIp(), event.getNewIp(), revokeUrl);
        });
    }
}
