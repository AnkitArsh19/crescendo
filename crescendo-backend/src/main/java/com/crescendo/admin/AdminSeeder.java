package com.crescendo.admin;

import com.crescendo.enums.UserRole;
import com.crescendo.user.user_command.User_commandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds admin users on application startup.
 *
 * Sources:
 *   1. crescendo.admin.emails property (comma-separated)
 *   2. admin_email table (dynamically added via Admin Panel)
 *
 * If any of those users already exist in the database, promotes them to ADMIN.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final User_commandRepository userRepo;
    private final AdminEmailRepository adminEmailRepo;
    private final List<String> configuredEmails;

    public AdminSeeder(User_commandRepository userRepo,
                       AdminEmailRepository adminEmailRepo,
                       @Value("${crescendo.admin.emails:}") String adminEmailsCsv) {
        this.userRepo = userRepo;
        this.adminEmailRepo = adminEmailRepo;
        this.configuredEmails = adminEmailsCsv.isBlank()
                ? List.of()
                : List.of(adminEmailsCsv.split(",")).stream().map(String::trim).toList();
    }

    @Override
    public void run(ApplicationArguments args) {
        // Merge property-configured emails into admin_email table
        for (String email : configuredEmails) {
            if (!email.isBlank() && !adminEmailRepo.existsByEmail(email.toLowerCase())) {
                adminEmailRepo.save(new AdminEmail(email, "system"));
                log.info("[admin] Seeded admin email from config: {}", email);
            }
        }

        // Now promote all users matching any admin email
        Set<String> allAdminEmails = new HashSet<>();
        adminEmailRepo.findAll().forEach(ae -> allAdminEmails.add(ae.getEmail()));

        if (allAdminEmails.isEmpty()) {
            log.info("[admin] No admin emails configured");
            return;
        }

        for (String email : allAdminEmails) {
            userRepo.findByEmailIgnoreCase(email).ifPresent(user -> {
                if (user.getRole() != UserRole.ADMIN) {
                    user.setRole(UserRole.ADMIN);
                    userRepo.save(user);
                    log.info("[admin] Promoted {} to ADMIN", email);
                }
            });
        }
    }
}
