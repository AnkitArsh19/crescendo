package com.crescendo.emailservice.dmarc;

import com.crescendo.emailservice.domain.Domain;
import com.crescendo.emailservice.domain.DomainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DmarcReportIngestionService {
    private static final Logger logger = LoggerFactory.getLogger(DmarcReportIngestionService.class);

    private final DmarcAlignmentReportRepository reportRepository;
    private final DomainRepository domainRepository;
    private final com.crescendo.shared.domain.event.DomainEventPublisher eventPublisher;

    public DmarcReportIngestionService(DmarcAlignmentReportRepository reportRepository, 
                                       DomainRepository domainRepository,
                                       com.crescendo.shared.domain.event.DomainEventPublisher eventPublisher) {
        this.reportRepository = reportRepository;
        this.domainRepository = domainRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void ingestReport(String xmlPayload) {
        // In a real application, we would parse the XML payload using javax.xml.parsers.DocumentBuilder
        // and extract all <record> elements, then save each as a DmarcAlignmentReport entity.
        // For demonstration, we simulate parsing here.

        logger.info("[dmarc-ingestion] Ingesting DMARC report...");

        // Simulate extraction of domain name from XML (e.g. <domain>example.com</domain>)
        // We'll just extract it using regex for simplicity here.
        String domainName = extractValue(xmlPayload, "domain");
        if (domainName == null) {
            logger.warn("[dmarc-ingestion] Could not find domain in DMARC report");
            return;
        }

        Domain domain = domainRepository.findByDomainNameAndStatus(domainName, com.crescendo.enums.DomainStatus.VERIFIED)
                .orElse(null);

        if (domain == null) {
            logger.warn("[dmarc-ingestion] Domain {} not found in system", domainName);
            return;
        }

        String orgName = extractValue(xmlPayload, "org_name");
        String reportId = extractValue(xmlPayload, "report_id");
        String sourceIp = extractValue(xmlPayload, "source_ip");
        String disposition = extractValue(xmlPayload, "disposition");
        boolean spfAligned = "pass".equalsIgnoreCase(extractValue(xmlPayload, "spf"));
        boolean dkimAligned = "pass".equalsIgnoreCase(extractValue(xmlPayload, "dkim"));

        DmarcAlignmentReport report = new DmarcAlignmentReport(
                UUID.randomUUID(),
                domain,
                orgName,
                reportId,
                sourceIp,
                spfAligned,
                dkimAligned,
                disposition,
                Instant.now().minusSeconds(86400),
                Instant.now()
        );

        reportRepository.save(report);

        // If disposition is reject or quarantine, or alignment failed, trigger event!
        if (!spfAligned && !dkimAligned) {
            // Trigger DmarcAlignmentFailureEvent
            logger.warn("[dmarc-ingestion] DMARC Alignment Failure detected for domain {}", domainName);
            
            eventPublisher.publish(new com.crescendo.emailservice.domain_event.DmarcAlignmentFailureEvent(
                    domain.getId(), domainName, orgName, sourceIp
            ));
        }
    }

    private String extractValue(String xml, String tag) {
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";
        int start = xml.indexOf(startTag);
        if (start == -1) return null;
        start += startTag.length();
        int end = xml.indexOf(endTag, start);
        if (end == -1) return null;
        return xml.substring(start, end).trim();
    }
}
