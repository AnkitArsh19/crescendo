package com.crescendo.admin;

import com.crescendo.enums.UserRole;
import com.crescendo.user.domain_event.UserRegisteredEvent;
import com.crescendo.user.user_command.User_commandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for new user registrations and auto-promotes users
 * whose email is in the admin whitelist.
 */
@Component
public class AdminEmailRegistrationListener {

    private static final Logger log = LoggerFactory.getLogger(AdminEmailRegistrationListener.class);

    private final AdminEmailRepository adminEmailRepo;
    private final User_commandRepository userRepo;

    public AdminEmailRegistrationListener(AdminEmailRepository adminEmailRepo,
                                           User_commandRepository userRepo) {
        this.adminEmailRepo = adminEmailRepo;
        this.userRepo = userRepo;
    }

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        String email = event.getEmail();
        if (email == null || email.isBlank()) return;

        String normalized = email.trim().toLowerCase();
        if (adminEmailRepo.existsByEmail(normalized)) {
            userRepo.findById(event.aggregateId()).ifPresent(user -> {
                if (user.getRole() != UserRole.ADMIN) {
                    user.setRole(UserRole.ADMIN);
                    userRepo.save(user);
                    log.info("[admin] Auto-promoted newly registered user {} to ADMIN", email);
                }
            });
        }
    }
}
