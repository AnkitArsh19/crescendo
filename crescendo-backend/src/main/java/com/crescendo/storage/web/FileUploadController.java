package com.crescendo.storage.web;

import com.crescendo.security.RateLimitingService;
import com.crescendo.security.access.AccessControlService;
import com.crescendo.security.access.PlatformLimits;
import com.crescendo.storage.FileStorageService;
import com.crescendo.storage.security.FileValidationService;
import com.crescendo.storage.storage_command.ConsumptionModel;
import com.crescendo.storage.storage_command.UploadedFile_command;
import com.crescendo.storage.storage_command.UploadedFile_commandRepository;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_query.User_query;
import com.crescendo.user.user_query.User_queryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.UUID;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    private final UploadedFile_commandRepository fileRepository;
    private final RateLimitingService rateLimitingService;
    private final AccessControlService accessControlService;
    private final User_queryRepository userQueryRepository;
    private final User_commandRepository userCommandRepository;

    public FileUploadController(FileStorageService fileStorageService,
                                FileValidationService fileValidationService,
                                UploadedFile_commandRepository fileRepository,
                                RateLimitingService rateLimitingService,
                                AccessControlService accessControlService,
                                User_queryRepository userQueryRepository,
                                User_commandRepository userCommandRepository) {
        this.fileStorageService = fileStorageService;
        this.fileValidationService = fileValidationService;
        this.fileRepository = fileRepository;
        this.rateLimitingService = rateLimitingService;
        this.accessControlService = accessControlService;
        this.userQueryRepository = userQueryRepository;
        this.userCommandRepository = userCommandRepository;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("consumptionModel") ConsumptionModel consumptionModel,
            @RequestParam(value = "maxSizeMB", required = false) Integer maxSizeMB,
            Authentication auth) throws IOException, NoSuchAlgorithmException {

        UUID userId = UUID.fromString(auth.getName());

        // 1. Rate Limiting (30 requests per minute)
        if (rateLimitingService.isRateLimited("upload", userId.toString(), 30, Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded for uploads");
        }

        // 2. Resolve tightest size limits
        PlatformLimits limits = accessControlService.limitsFor(accessControlService.currentTier());
        User_query user = userQueryRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        long availableQuota = limits.maxStorageBytes() - user.getStorageUsedBytes();

        long maxAllowedSize = Math.min(50L * 1024 * 1024, availableQuota);
        if (maxSizeMB != null && consumptionModel == ConsumptionModel.RETAINED) {
            maxAllowedSize = Math.min(maxAllowedSize, maxSizeMB * 1024L * 1024L);
        }

        // 3. Validation & Scanning
        fileValidationService.validate(file, consumptionModel, maxAllowedSize);

        // 4. Compute Checksum
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(file.getBytes());
        String checksum = HexFormat.of().formatHex(hashBytes);

        // 5. Storage Upload
        String storageKey = UUID.randomUUID().toString();
        fileStorageService.upload(file, storageKey);

        // 6. Record tracking entity and increment quota
        UploadedFile_command tracker = new UploadedFile_command(
                storageKey,
                userId,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getSize(),
                checksum,
                consumptionModel
        );
        fileRepository.save(tracker);

        User_command userCmd = userCommandRepository.findById(userId).orElseThrow();
        userCmd.setStorageUsedBytes(userCmd.getStorageUsedBytes() + file.getSize());
        userCommandRepository.save(userCmd);

        // 7. Return structured reference
        return ResponseEntity.ok(new UploadResponse(
                tracker.getOriginalName(),
                tracker.getContentType(),
                tracker.getSizeBytes(),
                tracker.getStorageKey(),
                tracker.getChecksum(),
                tracker.getConsumptionModel()
        ));
    }

    public record UploadResponse(
            String name,
            String contentType,
            long sizeBytes,
            String storageKey,
            String checksum,
            ConsumptionModel consumptionModel
    ) {}
}
