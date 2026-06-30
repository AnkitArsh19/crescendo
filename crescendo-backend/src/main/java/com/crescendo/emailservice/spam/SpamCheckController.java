package com.crescendo.emailservice.spam;

import com.crescendo.enums.EmailType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class SpamCheckController {

    private final EmailContentSpamCheckerService spamCheckerService;

    public SpamCheckController(EmailContentSpamCheckerService spamCheckerService) {
        this.spamCheckerService = spamCheckerService;
    }

    public record SpamCheckRequest(
            String subject,
            String htmlBody,
            String textBody,
            EmailType emailType
    ) {}

    @PostMapping("/check-content")
    public ResponseEntity<SpamCheckResult> checkContent(@RequestBody SpamCheckRequest request) {
        SpamCheckResult result = spamCheckerService.checkContent(
                request.subject(),
                request.htmlBody(),
                request.textBody(),
                request.emailType() != null ? request.emailType() : EmailType.MARKETING
        );
        return ResponseEntity.ok(result);
    }
}
