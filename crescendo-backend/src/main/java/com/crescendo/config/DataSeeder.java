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
            <body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#f4f4f5;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;box-shadow:0 8px 24px rgba(0,0,0,0.4);">
                    <tr><td style="background:linear-gradient(90deg, #6366f1, #a855f7);height:4px;"></td></tr>
                    <tr><td style="padding:40px 36px 20px 36px;">
                      <span style="display:inline-block;padding:4px 10px;background:#27272a;color:#a1a1aa;border-radius:20px;font-size:12px;font-weight:600;letter-spacing:0.5px;text-transform:uppercase;margin-bottom:16px;">Welcome</span>
                      <h1 style="color:#ffffff;font-size:24px;font-weight:700;margin:0 0 16px;">Welcome to {{company_name}}, {{first_name}}!</h1>
                      <p style="color:#a1a1aa;font-size:15px;line-height:1.6;margin:0 0 24px;">We're thrilled to have you join our automation community. Your account is ready to build workflows and orchestrate AI agents.</p>
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto 28px auto;">
                        <tr><td align="center" style="background:linear-gradient(135deg, #6366f1, #4f46e5);border-radius:8px;padding:12px 28px;">
                          <a href="{{dashboard_url}}" style="color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;">Open Dashboard &rarr;</a>
                        </td></tr>
                      </table>
                      <p style="color:#71717a;font-size:13px;line-height:1.5;margin:0;">If you have any questions, reply to this email &mdash; we're happy to help.</p>
                    </td></tr>
                    <tr><td style="background:#121215;padding:20px 36px;border-top:1px solid #27272a;text-align:center;">
                      <p style="color:#52525b;font-size:12px;margin:0;">&copy; {{year}} {{company_name}}. All rights reserved.</p>
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
            <body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#f4f4f5;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;">
                    <tr><td style="background:#ef4444;height:4px;"></td></tr>
                    <tr><td style="padding:36px;">
                      <span style="display:inline-block;padding:4px 10px;background:#451a1a;color:#fca5a5;border-radius:20px;font-size:12px;font-weight:600;margin-bottom:14px;">Security Notice</span>
                      <h1 style="color:#ffffff;font-size:22px;font-weight:700;margin:0 0 14px;">Reset your password</h1>
                      <p style="color:#a1a1aa;font-size:15px;line-height:1.6;margin:0 0 20px;">Hi {{first_name}}, we received a request to reset your password. Click below to choose a secure new one.</p>
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 24px 0;">
                        <tr><td style="background:#ef4444;border-radius:6px;padding:10px 24px;">
                          <a href="{{reset_link}}" style="color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;">Reset Password &rarr;</a>
                        </td></tr>
                      </table>
                      <p style="color:#71717a;font-size:12px;margin:0;">This link expires in {{expiry_hours}} hours. If you didn't request this, you can safely ignore this email.</p>
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
            <body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#f4f4f5;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;">
                    <tr><td style="background:#3b82f6;height:4px;"></td></tr>
                    <tr><td style="padding:36px;">
                      <h1 style="color:#ffffff;font-size:22px;font-weight:700;margin:0 0 8px;">Order Confirmed &#10003;</h1>
                      <p style="color:#a1a1aa;font-size:14px;margin:0 0 20px;">Order <strong>#{{order_id}}</strong> &bull; {{order_date}}</p>
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#27272a;border-radius:8px;padding:16px;margin-bottom:20px;">
                        <tr><td style="color:#d4d4d8;font-size:14px;padding:4px 0;"><strong>Items:</strong> {{item_count}}</td></tr>
                        <tr><td style="color:#60a5fa;font-size:16px;font-weight:700;padding:4px 0;">Total: {{order_total}}</td></tr>
                      </table>
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 16px 0;">
                        <tr><td style="background:#3b82f6;border-radius:6px;padding:10px 22px;">
                          <a href="{{order_url}}" style="color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;">View Order Details &rarr;</a>
                        </td></tr>
                      </table>
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
            <body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#f4f4f5;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;">
                    <tr><td style="background:linear-gradient(90deg, #8b5cf6, #ec4899);height:4px;"></td></tr>
                    <tr><td style="padding:36px;">
                      <span style="font-size:12px;font-weight:700;color:#c084fc;text-transform:uppercase;letter-spacing:1px;">{{newsletter_date}}</span>
                      <h1 style="color:#ffffff;font-size:24px;font-weight:700;margin:8px 0 16px 0;">{{newsletter_title}}</h1>
                      <p style="color:#a1a1aa;font-size:15px;line-height:1.6;margin:0 0 20px;">Hi {{first_name}}, {{newsletter_intro}}</p>
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border-left:4px solid #8b5cf6;padding-left:14px;margin-bottom:20px;">
                        <tr><td>
                          <h2 style="color:#ffffff;font-size:16px;margin:0 0 6px;">{{article_1_title}}</h2>
                          <p style="color:#a1a1aa;font-size:14px;line-height:1.5;margin:0 0 8px;">{{article_1_summary}}</p>
                          <a href="{{article_1_url}}" style="color:#c084fc;font-size:13px;font-weight:600;text-decoration:none;">Read full story &rarr;</a>
                        </td></tr>
                      </table>
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
            <body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#f4f4f5;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;">
                    <tr><td style="background:linear-gradient(90deg, #f59e0b, #ec4899);height:4px;"></td></tr>
                    <tr><td style="padding:36px;">
                      <span style="display:inline-block;padding:4px 10px;background:#451a03;color:#fcd34d;border-radius:20px;font-size:12px;font-weight:600;margin-bottom:14px;">Special Invitation</span>
                      <h1 style="color:#ffffff;font-size:24px;font-weight:700;margin:0 0 12px;">You're invited: {{event_name}}</h1>
                      <p style="color:#a1a1aa;font-size:15px;line-height:1.6;margin:0 0 20px;">Hi {{first_name}}, you are cordially invited to attend <strong>{{event_name}}</strong>.</p>
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#27272a;border-radius:8px;padding:16px;margin-bottom:20px;">
                        <tr><td style="color:#e4e4e7;font-size:13px;padding:4px 0;">&#128197; <strong>Date & Time:</strong> {{event_date}} at {{event_time}}</td></tr>
                        <tr><td style="color:#e4e4e7;font-size:13px;padding:4px 0;">&#128205; <strong>Location:</strong> {{event_location}}</td></tr>
                      </table>
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 16px 0;">
                        <tr><td style="background:#f59e0b;border-radius:6px;padding:10px 24px;">
                          <a href="{{rsvp_url}}" style="color:#18181b;text-decoration:none;font-size:14px;font-weight:700;">RSVP Now &rarr;</a>
                        </td></tr>
                      </table>
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
            <body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#f4f4f5;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;">
                    <tr><td style="background:#3b82f6;height:4px;"></td></tr>
                    <tr><td style="padding:32px;">
                      <h1 style="color:#ffffff;font-size:20px;font-weight:700;margin:0 0 12px;">&#128276; {{alert_title}}</h1>
                      <p style="color:#a1a1aa;font-size:15px;line-height:1.6;margin:0 0 20px;">{{alert_message}}</p>
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 12px 0;">
                        <tr><td style="background:#3b82f6;border-radius:6px;padding:10px 22px;">
                          <a href="{{action_url}}" style="color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;">{{action_label}}</a>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;
    }
}
