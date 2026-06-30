package com.crescendo.emailservice.domain;

import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.enums.DomainSendReadiness;
import com.crescendo.enums.EmailStatus;
import com.crescendo.enums.WarmingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service responsible for governing the domain warming process.
 * Evaluates bounce and complaint rates over a 48-hour rolling window and
 * exponentially increases (doubles) the daily send cap, or downgrades it on spikes.
 */
@Service
public class DomainWarmingService {

    private static final Logger log = LoggerFactory.getLogger(DomainWarmingService.class);
    
    public static final double MAX_BOUNCE_RATE = 0.05; // 5%
    public static final double MAX_COMPLAINT_RATE = 0.001; // 0.1%
    private static final int MAX_AUTO_CAP = 5000;
    private static final int STARTING_CAP = 50;

    private final DomainRepository domainRepository;
    private final EmailLogRepository emailLogRepository;

    public DomainWarmingService(DomainRepository domainRepository, EmailLogRepository emailLogRepository) {
        this.domainRepository = domainRepository;
        this.emailLogRepository = emailLogRepository;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void evaluateDomainWarming() {
        log.info("[DomainWarming] Starting daily evaluation of domain sending limits.");
        
        List<Domain> readyDomains = domainRepository.findBySendReadiness(DomainSendReadiness.READY);
        Instant fortyEightHoursAgo = Instant.now().minus(48, ChronoUnit.HOURS);

        for (Domain domain : readyDomains) {
            String domainName = domain.getDomainName();
            long totalSends = emailLogRepository.countTotalSendsSince(domain.getUser().getId(), domainName, fortyEightHoursAgo);
            
            if (totalSends == 0) {
                // No sends in the last 48 hours, keep cap as is
                continue;
            }

            long bounces = emailLogRepository.countStatusSince(domain.getUser().getId(), domainName, EmailStatus.BOUNCED, fortyEightHoursAgo);
            long complaints = emailLogRepository.countStatusSince(domain.getUser().getId(), domainName, EmailStatus.COMPLAINED, fortyEightHoursAgo);

            double bounceRate = (double) bounces / totalSends;
            double complaintRate = (double) complaints / totalSends;

            log.info("[DomainWarming] Domain {}: {} sends, {} bounces ({}%), {} complaints ({}%)",
                    domainName, totalSends, bounces, bounceRate * 100, complaints, complaintRate * 100);

            if (bounceRate > MAX_BOUNCE_RATE || complaintRate > MAX_COMPLAINT_RATE) {
                // Spike detected, downgrade!
                log.warn("[DomainWarming] Domain {} spiked! Downgrading to warming status.", domainName);
                domain.setWarmingStatus(WarmingStatus.WARMING_UP);
                domain.setDailySendCap(STARTING_CAP);
                domain.updateReadiness();
                domainRepository.save(domain);
                continue;
            }

            if (domain.getWarmingStatus() == WarmingStatus.WARMING_UP) {
                // Clean window, increase cap
                int newCap = domain.getDailySendCap() * 2;
                if (newCap >= MAX_AUTO_CAP) {
                    newCap = MAX_AUTO_CAP;
                    domain.setWarmingStatus(WarmingStatus.MATURE);
                    log.info("[DomainWarming] Domain {} reached MATURE status.", domainName);
                }
                
                domain.setDailySendCap(newCap);
                domain.updateReadiness();
                domainRepository.save(domain);
                log.info("[DomainWarming] Domain {} cap increased to {}", domainName, newCap);
            }
        }
        
        log.info("[DomainWarming] Daily evaluation complete.");
    }
}
