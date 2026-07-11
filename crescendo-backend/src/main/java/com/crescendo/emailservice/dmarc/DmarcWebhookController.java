package com.crescendo.emailservice.dmarc;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/dmarc")
public class DmarcWebhookController {

    private final DmarcReportIngestionService ingestionService;

    public DmarcWebhookController(DmarcReportIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = {"application/xml", "text/xml", "application/json", "text/plain"})
    public ResponseEntity<Void> receiveDmarcReport(@RequestBody String payload) {
        // payload could be an XML string directly or base64 encoded depending on the email webhook provider.
        // For our purpose, we pass it directly to ingestion service.
        ingestionService.ingestReport(payload);
        return ResponseEntity.ok().build();
    }
}
