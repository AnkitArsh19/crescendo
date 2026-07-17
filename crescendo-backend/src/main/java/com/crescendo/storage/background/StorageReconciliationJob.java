package com.crescendo.storage.background;

import com.crescendo.storage.storage_command.FileStatus;
import com.crescendo.storage.storage_command.UploadedFile_commandRepository;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class StorageReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(StorageReconciliationJob.class);

    private final User_commandRepository userRepository;
    private final UploadedFile_commandRepository fileRepository;

    public StorageReconciliationJob(User_commandRepository userRepository, UploadedFile_commandRepository fileRepository) {
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
    }

    @Scheduled(cron = "0 0 2 * * *") // Runs daily at 2:00 AM
    public void reconcileStorageQuotas() {
        log.info("Starting periodic storage reconciliation job");
        
        List<User_command> users = userRepository.findAll();
        int correctedCount = 0;

        for (User_command user : users) {
            boolean corrected = reconcileUserQuota(user.getId());
            if (corrected) {
                correctedCount++;
            }
        }
        
        log.info("Completed storage reconciliation job. Corrected {} drifted quotas.", correctedCount);
    }

    @Transactional
    public boolean reconcileUserQuota(UUID userId) {
        User_command user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        Long actualStorageBytes = fileRepository.sumBytesByUserIdAndStatusNot(userId, FileStatus.CONSUMED);
        if (actualStorageBytes == null) actualStorageBytes = 0L;

        if (!user.getStorageUsedBytes().equals(actualStorageBytes)) {
            log.warn("Storage drift detected for user {}. Tracked: {}, Actual: {}. Self-healing.",
                    userId, user.getStorageUsedBytes(), actualStorageBytes);
            user.setStorageUsedBytes(actualStorageBytes);
            userRepository.save(user);
            return true;
        }

        return false;
    }
}
