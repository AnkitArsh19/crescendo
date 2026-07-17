package com.crescendo.storage.background;

import com.crescendo.storage.FileStorageService;
import com.crescendo.storage.storage_command.UploadedFile_command;
import com.crescendo.storage.storage_command.UploadedFile_commandRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class UploadReaperJob {
    private static final Logger log = LoggerFactory.getLogger(UploadReaperJob.class);

    private final UploadedFile_commandRepository fileRepository;
    private final FileStorageService fileStorageService;

    public UploadReaperJob(UploadedFile_commandRepository fileRepository, FileStorageService fileStorageService) {
        this.fileRepository = fileRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Runs every 15 minutes.
     * 1. Deletes PENDING uploads older than 24 hours (abandoned frontend uploads).
     * 2. Deletes RELAY uploads older than 4 hours that haven't been CONSUMED (failed relays).
     */
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void reapStaleUploads() {
        log.info("Starting stale upload reaper job...");
        int count = 0;

        // 1. PENDING > 24 hours
        Instant pendingThreshold = Instant.now().minus(24, ChronoUnit.HOURS);
        List<UploadedFile_command> abandoned = fileRepository.findPendingBefore(pendingThreshold);
        for (UploadedFile_command file : abandoned) {
            fileStorageService.delete(file.getStorageKey());
            fileRepository.delete(file);
            count++;
        }

        // 2. RELAY > 4 hours (failed relays that never reached CONSUMED)
        Instant relayThreshold = Instant.now().minus(4, ChronoUnit.HOURS);
        List<UploadedFile_command> failedRelays = fileRepository.findStaleRelaysBefore(relayThreshold);
        for (UploadedFile_command file : failedRelays) {
            fileStorageService.delete(file.getStorageKey());
            fileRepository.delete(file);
            count++;
        }

        log.info("Upload reaper job finished. Reaped {} stale files.", count);
    }
}
