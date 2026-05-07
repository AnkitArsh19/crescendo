package com.crescendo.config;

import com.crescendo.app.App;
import com.crescendo.app.AppRepository;
import com.crescendo.apps.AppDefinition;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandRepository;
import com.crescendo.emailservice.emailtemplate.template_query.EmailTemplate_query;
import com.crescendo.emailservice.emailtemplate.template_query.EmailTemplate_queryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Seeds the database with starter data on application startup.
 * Idempotent — skips seeding if data already exists.
 *
 * Seeds:
 *   1. App catalog (Phase 1 integrations)
 *   2. Starter email templates (system-level, userId = SYSTEM_USER_ID)
 */
@Component
public class DataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    /**
     * Well-known system user ID for starter/gallery templates.
     * Templates with this userId are visible to all users as read-only starters.
     */
    public static final UUID SYSTEM_USER_ID = new UUID(0, 0);

    private final AppRepository appRepository;
    private final EmailTemplate_commandRepository templateCommandRepo;
    private final EmailTemplate_queryRepository templateQueryRepo;
    private final List<AppDefinition> appDefinitions;
    private final RedisTemplate<String, Object> redisTemplate;

    public DataSeeder(AppRepository appRepository,
                      EmailTemplate_commandRepository templateCommandRepo,
                      EmailTemplate_queryRepository templateQueryRepo,
                      List<AppDefinition> appDefinitions,
                      RedisTemplate<String, Object> redisTemplate) {
        this.appRepository = appRepository;
        this.templateCommandRepo = templateCommandRepo;
        this.templateQueryRepo = templateQueryRepo;
        this.appDefinitions = appDefinitions;
        this.redisTemplate = redisTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        // Use RedisTemplate.delete() directly - bypasses TransactionAwareCacheDecorator
        // (cacheManager.getCache().clear() is deferred to afterCommit when inside @Transactional,
        // so a racing request can still hit the stale key before commit completes).
      Set<String> staleKeys = new HashSet<>();
      Set<String> legacyKeys = redisTemplate.keys("apps::*");
      Set<String> v2Keys = redisTemplate.keys("v2:apps::*");
      if (legacyKeys != null) staleKeys.addAll(legacyKeys);
      if (v2Keys != null) staleKeys.addAll(v2Keys);
      if (!staleKeys.isEmpty()) {
           redisTemplate.delete(staleKeys);
           logger.info("[seeder] Evicted {} stale app cache keys from Redis", staleKeys.size());
      }

      seedAppCatalog();
      seedStarterTemplates();
    }

    // ─── App Catalog ──────────────────────────────────────────────

    private void seedAppCatalog() {
        List<App> apps = appDefinitions.stream()
                .map(AppDefinition::toApp)
                .toList();

        // Upsert: saveAll uses merge semantics — inserts new apps and updates
        // existing ones with the latest metadata from AppDefinition beans.
        // This ensures new fields (credentialSchema, category, helpUrl, etc.)
        // are populated even on databases that already have app rows.
        appRepository.saveAll(apps);
        logger.info("[seeder] Upserted {} apps into catalog (auto-discovered)", apps.size());

        // ── Stale app cleanup ──────────────────────────────────────
        // Remove any DB rows whose appKey no longer has an AppDefinition bean.
        // This happens when an integration is removed from the codebase (e.g. Twitch).
        Set<String> validAppKeys = apps.stream()
                .map(App::getAppKey)
                .collect(java.util.stream.Collectors.toSet());

        List<App> allDbApps = appRepository.findAll();
        List<App> staleApps = allDbApps.stream()
                .filter(a -> !validAppKeys.contains(a.getAppKey()))
                .toList();

        if (!staleApps.isEmpty()) {
            appRepository.deleteAll(staleApps);
            staleApps.forEach(a ->
                logger.info("[seeder] Removed stale app from catalog: {}", a.getAppKey())
            );
        }
    }

    // ─── Starter Email Templates ──────────────────────────────────

    private void seedStarterTemplates() {
        if (!templateCommandRepo.findByUserIdOrderByCreatedAtDesc(SYSTEM_USER_ID).isEmpty()) {
            logger.debug("[seeder] Starter templates already exist, skipping");
            return;
        }

        List<TemplateDefinition> templates = List.of(
            new TemplateDefinition(
                "Welcome Email",
                "Welcome to {{company_name}}!",
                welcomeEmailHtml(),
                "Welcome to {{company_name}}! We're excited to have you on board."
            ),
            new TemplateDefinition(
                "Password Reset",
                "Reset your password",
                passwordResetHtml(),
                "You requested a password reset. Click the link to reset your password: {{reset_link}}"
            ),
            new TemplateDefinition(
                "Order Confirmation",
                "Order #{{order_id}} confirmed",
                orderConfirmationHtml(),
                "Your order #{{order_id}} has been confirmed. Total: {{order_total}}"
            ),
            new TemplateDefinition(
                "Newsletter",
                "{{newsletter_title}}",
                newsletterHtml(),
                "{{newsletter_title}} — {{newsletter_preview}}"
            ),
            new TemplateDefinition(
                "Event Invitation",
                "You're invited: {{event_name}}",
                eventInvitationHtml(),
                "You're invited to {{event_name}} on {{event_date}}. {{event_description}}"
            ),
            new TemplateDefinition(
                "Notification Alert",
                "{{alert_title}}",
                notificationAlertHtml(),
                "{{alert_title}} — {{alert_message}}"
            )
        );

        for (TemplateDefinition def : templates) {
            UUID id = UUID.randomUUID();
            templateCommandRepo.save(new EmailTemplate_command(
                    id, SYSTEM_USER_ID, def.name, def.subject, def.htmlBody, def.textBody));
            templateQueryRepo.save(new EmailTemplate_query(
                    id, SYSTEM_USER_ID, def.name, def.subject, def.htmlBody, def.textBody));
        }

        logger.info("[seeder] Seeded {} starter email templates", templates.size());
    }

    private record TemplateDefinition(String name, String subject, String htmlBody, String textBody) {}

    // ── Template HTML ──────────────────────────────────────────────

    private String welcomeEmailHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f5;padding:40px 20px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr><td style="background-color:#6366f1;padding:32px 40px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:24px;">Welcome to {{company_name}}!</h1>
                    </td></tr>
                    <!-- Body -->
                    <tr><td style="padding:40px;">
                      <p style="color:#374151;font-size:16px;line-height:1.6;margin:0 0 16px;">Hi {{first_name}},</p>
                      <p style="color:#374151;font-size:16px;line-height:1.6;margin:0 0 24px;">We're thrilled to have you on board. Your account is all set up and ready to go.</p>
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto;">
                        <tr><td style="background-color:#6366f1;border-radius:6px;padding:12px 32px;">
                          <a href="{{dashboard_url}}" style="color:#ffffff;text-decoration:none;font-size:16px;font-weight:600;">Get Started</a>
                        </td></tr>
                      </table>
                      <p style="color:#6b7280;font-size:14px;line-height:1.6;margin:24px 0 0;">If you have any questions, reply to this email — we're happy to help.</p>
                    </td></tr>
                    <!-- Footer -->
                    <tr><td style="background-color:#f9fafb;padding:24px 40px;text-align:center;border-top:1px solid #e5e7eb;">
                      <p style="color:#9ca3af;font-size:12px;margin:0;">&copy; {{year}} {{company_name}}. All rights reserved.</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;
    }

    private String passwordResetHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f5;padding:40px 20px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
                    <tr><td style="background-color:#ef4444;padding:32px 40px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:24px;">Password Reset</h1>
                    </td></tr>
                    <tr><td style="padding:40px;">
                      <p style="color:#374151;font-size:16px;line-height:1.6;margin:0 0 16px;">Hi {{first_name}},</p>
                      <p style="color:#374151;font-size:16px;line-height:1.6;margin:0 0 24px;">We received a request to reset your password. Click the button below to choose a new one.</p>
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto;">
                        <tr><td style="background-color:#ef4444;border-radius:6px;padding:12px 32px;">
                          <a href="{{reset_link}}" style="color:#ffffff;text-decoration:none;font-size:16px;font-weight:600;">Reset Password</a>
                        </td></tr>
                      </table>
                      <p style="color:#6b7280;font-size:14px;line-height:1.6;margin:24px 0 0;">This link expires in {{expiry_hours}} hours. If you didn't request this, you can safely ignore this email.</p>
                    </td></tr>
                    <tr><td style="background-color:#f9fafb;padding:24px 40px;text-align:center;border-top:1px solid #e5e7eb;">
                      <p style="color:#9ca3af;font-size:12px;margin:0;">&copy; {{year}} {{company_name}}</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;
    }

    private String orderConfirmationHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f5;padding:40px 20px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
                    <tr><td style="background-color:#10b981;padding:32px 40px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:24px;">Order Confirmed &#10003;</h1>
                    </td></tr>
                    <tr><td style="padding:40px;">
                      <p style="color:#374151;font-size:16px;line-height:1.6;margin:0 0 16px;">Hi {{first_name}},</p>
                      <p style="color:#374151;font-size:16px;line-height:1.6;margin:0 0 24px;">Your order <strong>#{{order_id}}</strong> has been confirmed and is being processed.</p>
                      <!-- Order Summary -->
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f9fafb;border-radius:6px;padding:20px;margin:0 0 24px;">
                        <tr><td>
                          <p style="color:#374151;font-size:14px;margin:0 0 8px;"><strong>Order ID:</strong> {{order_id}}</p>
                          <p style="color:#374151;font-size:14px;margin:0 0 8px;"><strong>Date:</strong> {{order_date}}</p>
                          <p style="color:#374151;font-size:14px;margin:0 0 8px;"><strong>Items:</strong> {{item_count}}</p>
                          <p style="color:#374151;font-size:16px;margin:8px 0 0;"><strong>Total: {{order_total}}</strong></p>
                        </td></tr>
                      </table>
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto;">
                        <tr><td style="background-color:#10b981;border-radius:6px;padding:12px 32px;">
                          <a href="{{order_url}}" style="color:#ffffff;text-decoration:none;font-size:16px;font-weight:600;">View Order</a>
                        </td></tr>
                      </table>
                    </td></tr>
                    <tr><td style="background-color:#f9fafb;padding:24px 40px;text-align:center;border-top:1px solid #e5e7eb;">
                      <p style="color:#9ca3af;font-size:12px;margin:0;">&copy; {{year}} {{company_name}}</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;
    }

    private String newsletterHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f5;padding:40px 20px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
                    <tr><td style="background-color:#8b5cf6;padding:32px 40px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:24px;">{{newsletter_title}}</h1>
                      <p style="color:#e0d5f7;font-size:14px;margin:8px 0 0;">{{newsletter_date}}</p>
                    </td></tr>
                    <tr><td style="padding:40px;">
                      <p style="color:#374151;font-size:16px;line-height:1.6;margin:0 0 16px;">Hi {{first_name}},</p>
                      <p style="color:#374151;font-size:16px;line-height:1.6;margin:0 0 24px;">{{newsletter_intro}}</p>
                      <!-- Article 1 -->
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border-left:4px solid #8b5cf6;padding-left:16px;margin:0 0 24px;">
                        <tr><td>
                          <h2 style="color:#374151;font-size:18px;margin:0 0 8px;">{{article_1_title}}</h2>
                          <p style="color:#6b7280;font-size:14px;line-height:1.5;margin:0 0 8px;">{{article_1_summary}}</p>
                          <a href="{{article_1_url}}" style="color:#8b5cf6;font-size:14px;font-weight:600;text-decoration:none;">Read more &rarr;</a>
                        </td></tr>
                      </table>
                      <!-- Article 2 -->
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border-left:4px solid #8b5cf6;padding-left:16px;margin:0 0 24px;">
                        <tr><td>
                          <h2 style="color:#374151;font-size:18px;margin:0 0 8px;">{{article_2_title}}</h2>
                          <p style="color:#6b7280;font-size:14px;line-height:1.5;margin:0 0 8px;">{{article_2_summary}}</p>
                          <a href="{{article_2_url}}" style="color:#8b5cf6;font-size:14px;font-weight:600;text-decoration:none;">Read more &rarr;</a>
                        </td></tr>
                      </table>
                    </td></tr>
                    <tr><td style="background-color:#f9fafb;padding:24px 40px;text-align:center;border-top:1px solid #e5e7eb;">
                      <p style="color:#9ca3af;font-size:12px;margin:0 0 8px;">&copy; {{year}} {{company_name}}</p>
                      <a href="{{unsubscribe_url}}" style="color:#9ca3af;font-size:12px;text-decoration:underline;">Unsubscribe</a>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;
    }

    private String eventInvitationHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f5;padding:40px 20px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
                    <tr><td style="background-color:#f59e0b;padding:32px 40px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:24px;">You're Invited!</h1>
                    </td></tr>
                    <tr><td style="padding:40px;">
                      <p style="color:#374151;font-size:16px;line-height:1.6;margin:0 0 16px;">Hi {{first_name}},</p>
                      <p style="color:#374151;font-size:16px;line-height:1.6;margin:0 0 24px;">You're invited to <strong>{{event_name}}</strong>!</p>
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#fffbeb;border-radius:6px;padding:20px;margin:0 0 24px;border:1px solid #fde68a;">
                        <tr><td>
                          <p style="color:#374151;font-size:14px;margin:0 0 8px;">&#128197; <strong>Date:</strong> {{event_date}}</p>
                          <p style="color:#374151;font-size:14px;margin:0 0 8px;">&#128336; <strong>Time:</strong> {{event_time}}</p>
                          <p style="color:#374151;font-size:14px;margin:0 0 8px;">&#128205; <strong>Location:</strong> {{event_location}}</p>
                          <p style="color:#6b7280;font-size:14px;line-height:1.5;margin:8px 0 0;">{{event_description}}</p>
                        </td></tr>
                      </table>
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto;">
                        <tr><td style="background-color:#f59e0b;border-radius:6px;padding:12px 32px;">
                          <a href="{{rsvp_url}}" style="color:#ffffff;text-decoration:none;font-size:16px;font-weight:600;">RSVP Now</a>
                        </td></tr>
                      </table>
                    </td></tr>
                    <tr><td style="background-color:#f9fafb;padding:24px 40px;text-align:center;border-top:1px solid #e5e7eb;">
                      <p style="color:#9ca3af;font-size:12px;margin:0;">&copy; {{year}} {{company_name}}</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;
    }

    private String notificationAlertHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f5;padding:40px 20px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
                    <tr><td style="background-color:#3b82f6;padding:24px 40px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:20px;">&#128276; {{alert_title}}</h1>
                    </td></tr>
                    <tr><td style="padding:32px 40px;">
                      <p style="color:#374151;font-size:16px;line-height:1.6;margin:0 0 16px;">{{alert_message}}</p>
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto;">
                        <tr><td style="background-color:#3b82f6;border-radius:6px;padding:12px 32px;">
                          <a href="{{action_url}}" style="color:#ffffff;text-decoration:none;font-size:16px;font-weight:600;">{{action_label}}</a>
                        </td></tr>
                      </table>
                    </td></tr>
                    <tr><td style="background-color:#f9fafb;padding:20px 40px;text-align:center;border-top:1px solid #e5e7eb;">
                      <p style="color:#9ca3af;font-size:12px;margin:0;">&copy; {{year}} {{company_name}}</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;
    }
}
