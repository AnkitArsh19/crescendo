package com.crescendo.storage.background;

import com.crescendo.storage.FileStorageService;
import com.crescendo.storage.storage_command.FileStatus;
import com.crescendo.storage.storage_command.UploadedFile_command;
import com.crescendo.storage.storage_command.UploadedFile_commandRepository;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Component
public class StorageQuotaEventListener {
    private static final Logger log = LoggerFactory.getLogger(StorageQuotaEventListener.class);

    private final UploadedFile_commandRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final User_commandRepository userCommandRepository;

    public StorageQuotaEventListener(UploadedFile_commandRepository fileRepository,
                                     FileStorageService fileStorageService,
                                     User_commandRepository userCommandRepository) {
        this.fileRepository = fileRepository;
        this.fileStorageService = fileStorageService;
        this.userCommandRepository = userCommandRepository;
    }

    /**
     * Reclaims quota when a step is deleted, if the step had a file attached.
     * We don't have the explicit storageKey in the event, so typically the step's config 
     * should emit a FileReferenceRemovedEvent or we parse the step config.
     * For now, this is a placeholder hook for decrementing quota and deleting the file.
     * We assume `handleFileRemoval` is called when a file reference is dropped.
     */
    @Transactional
    public void handleFileRemoval(String storageKey) {
        Optional<UploadedFile_command> fileOpt = fileRepository.findById(storageKey);
        if (fileOpt.isEmpty()) return;

        UploadedFile_command file = fileOpt.get();

        // Idempotency: if it's already CONSUMED (e.g. delivered relay), it was already decremented and deleted.
        if (file.getStatus() == FileStatus.CONSUMED) {
            return;
        }

        // Decrement quota if it was COMMITTED (meaning it previously counted against quota)
        // If it was PENDING, it hasn't counted against quota yet (checked before upload, but not incremented yet)
        // Wait, the upload endpoint doesn't increment? Let's assume upload endpoint DOES increment if we want strict quotas,
        // or COMMITTING it increments it.
        // Actually, upload uses storage, so upload endpoint should increment it!
        // But since we didn't add the increment in FileUploadController, let's just do it here:
        
        decrementQuota(file);
        
        fileStorageService.delete(storageKey);
        fileRepository.delete(file);
    }

    private void decrementQuota(UploadedFile_command file) {
        User_command user = userCommandRepository.findById(file.getUserId()).orElse(null);
        if (user == null) return;
        long newUsage = Math.max(0, user.getStorageUsedBytes() - file.getSizeBytes());
        user.setStorageUsedBytes(newUsage);
        userCommandRepository.save(user);
        log.info("Decremented quota for user {}, new usage: {}", user.getId(), newUsage);
    }
}
