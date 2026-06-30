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

    public EmailUnsubscribeController(EmailSuppressionService suppressionService) {
        this.suppressionService = suppressionService;
    }

    /**
     * RFC 8058 One-Click Unsubscribe.
     * Must be a POST request. Must return 2xx immediately with no UI or confirmation page.
     */
    @PostMapping("/{emailLogId}")
    public ResponseEntity<Void> oneClickUnsubscribe(@PathVariable UUID emailLogId) {
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
    @GetMapping("/{emailLogId}")
    public ResponseEntity<String> visualUnsubscribe(@PathVariable UUID emailLogId) {
        boolean suppressed = suppressionService.unsubscribeByEmailLogId(emailLogId);
        
        // In a real app, this would return an HTML template or redirect to a frontend page.
        // For now, we return a simple HTML confirmation.
        String htmlResponse = """
            <!DOCTYPE html>
            <html>
            <head><title>Unsubscribed</title></head>
            <body style="font-family: sans-serif; text-align: center; padding-top: 50px;">
                <h2>You have been unsubscribed.</h2>
                <p>You will no longer receive marketing emails from this sender.</p>
            </body>
            </html>
            """;
            
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
