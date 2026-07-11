package com.crescendo.emailservice.webhook;

import com.crescendo.emailservice.suppression.EmailSuppressionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/unsubscribe")
public class EmailUnsubscribeController {

    private final EmailSuppressionService suppressionService;
    private final com.crescendo.emailservice.email_log.EmailLogRepository emailLogRepo;
    private final com.crescendo.emailservice.domain.DomainRepository domainRepo;

    public EmailUnsubscribeController(EmailSuppressionService suppressionService,
                                      com.crescendo.emailservice.email_log.EmailLogRepository emailLogRepo,
                                      com.crescendo.emailservice.domain.DomainRepository domainRepo) {
        this.suppressionService = suppressionService;
        this.emailLogRepo = emailLogRepo;
        this.domainRepo = domainRepo;
    }

    /**
     * RFC 8058 One-Click Unsubscribe.
     * Must be a POST request. Must return 2xx immediately with no UI or confirmation page.
     */
    @PostMapping
    public ResponseEntity<Void> oneClickUnsubscribe(@RequestParam("token") String token) {
        UUID emailLogId = suppressionService.verifyUnsubscribeToken(token);
        if (emailLogId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        boolean suppressed = suppressionService.unsubscribeByEmailLogId(emailLogId);
        if (!suppressed) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Human-clicked Unsubscribe link from the email footer.
     * Must be a GET request. Should render a confirmation page.
     */
    @GetMapping
    public ResponseEntity<String> visualUnsubscribe(@RequestParam("token") String token) {
        UUID emailLogId = suppressionService.verifyUnsubscribeToken(token);
        if (emailLogId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "text/html")
                    .body("<html><body><h2>Invalid or expired unsubscribe link.</h2></body></html>");
        }
        boolean suppressed = suppressionService.unsubscribeByEmailLogId(emailLogId);
        
        String logoHtml = "";
        String titleColor = "#000000";
        String copyHtml = "<p>You will no longer receive marketing emails from this sender.</p>";

        if (suppressed) {
            var logOpt = emailLogRepo.findById(emailLogId);
            if (logOpt.isPresent()) {
                var log = logOpt.get();
                String from = log.getFromAddress();
                int atIndex = from.indexOf('@');
                if (atIndex != -1 && atIndex < from.length() - 1) {
                    String domainName = from.substring(atIndex + 1);
                    var domainOpt = domainRepo.findByDomainNameAndUserId(domainName, log.getUserId());
                    if (domainOpt.isPresent()) {
                        var domain = domainOpt.get();
                        if (domain.getUnsubscribeLogoUrl() != null && !domain.getUnsubscribeLogoUrl().isBlank()) {
                            logoHtml = "<img src=\"" + domain.getUnsubscribeLogoUrl() + "\" style=\"max-height: 80px; margin-bottom: 20px;\" alt=\"Logo\" />";
                        }
                        if (domain.getUnsubscribePrimaryColor() != null && !domain.getUnsubscribePrimaryColor().isBlank()) {
                            titleColor = domain.getUnsubscribePrimaryColor();
                        }
                        if (domain.getUnsubscribeCopy() != null && !domain.getUnsubscribeCopy().isBlank()) {
                            copyHtml = "<p>" + domain.getUnsubscribeCopy().replace("\n", "<br>") + "</p>";
                        }
                    }
                }
            }
        }

        String htmlResponse = """
            <!DOCTYPE html>
            <html>
            <head><title>Unsubscribed</title></head>
            <body style="font-family: sans-serif; text-align: center; padding-top: 50px;">
                %s
                <h2 style="color: %s;">You have been unsubscribed.</h2>
                %s
            </body>
            </html>
            """.formatted(logoHtml, titleColor, copyHtml);
            
        if (!suppressed) {
            htmlResponse = """
                <!DOCTYPE html>
                <html>
                <head><title>Not Found</title></head>
                <body style="font-family: sans-serif; text-align: center; padding-top: 50px;">
                    <h2>Unsubscribe link invalid or expired.</h2>
                </body>
                </html>
                """;
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "text/html")
                    .body(htmlResponse);
        }

        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(htmlResponse);
    }
}
