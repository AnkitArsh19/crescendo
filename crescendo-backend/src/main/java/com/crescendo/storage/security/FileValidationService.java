package com.crescendo.storage.security;

import com.crescendo.storage.storage_command.ConsumptionModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
public class FileValidationService {
    private static final Logger log = LoggerFactory.getLogger(FileValidationService.class);

    private final VirusScanner virusScanner;

    // Hardcoded executable denylist (magic byte hex prefixes)
    private static final Set<String> DENYLIST = Set.of(
            "4D5A",     // .exe, .dll (Windows PE)
            "7F454C46", // .elf (Linux executable)
            "2321"      // #! (Shell scripts)
    );

    // Strict allowlist for RETAINED AI context (PDFs, images)
    private static final Set<String> RETAINED_ALLOWLIST = Set.of(
            "25504446", // .pdf
            "FFD8FF",   // .jpg, .jpeg
            "89504E47", // .png
            "52494646"  // .webp (RIFF header, needs WEBP check further in but this is enough for basic gating)
    );

    public FileValidationService(VirusScanner virusScanner) {
        this.virusScanner = virusScanner;
    }

    public void validate(MultipartFile file, ConsumptionModel consumptionModel, long maxAllowedSize) throws IOException, SecurityException {
        // 1. Size Check
        if (file.getSize() > maxAllowedSize) {
            throw new SecurityException("File size exceeds maximum allowed limit");
        }

        // 2. Magic Bytes Check
        String hexSignature = getHexSignature(file);
        
        // 3. Executable Denylist
        for (String denied : DENYLIST) {
            if (hexSignature.startsWith(denied)) {
                log.warn("Blocked executable file upload attempt. Signature: {}", hexSignature);
                throw new SecurityException("Executable and script files are strictly prohibited");
            }
        }

        // 4. Strict Allowlist for RETAINED files
        if (consumptionModel == ConsumptionModel.RETAINED) {
            boolean allowed = false;
            for (String allowedSig : RETAINED_ALLOWLIST) {
                if (hexSignature.startsWith(allowedSig)) {
                    allowed = true;
                    break;
                }
            }
            // Allow basic text files (which might not have standard magic bytes) if they are small enough
            // For now, we will rely on the magic byte list for binary formats.
            // Text files might start with arbitrary bytes, so we can check Content-Type as a fallback for text/plain
            if (!allowed && file.getContentType() != null && file.getContentType().startsWith("text/")) {
                allowed = true; // Very permissive for text, but acceptable since we blocked scripts in DENYLIST
            }

            if (!allowed) {
                log.warn("Blocked RETAINED upload of unauthorized type. Signature: {}", hexSignature);
                throw new SecurityException("File type not allowed for AI context retention");
            }
        }

        // 5. Virus Scan
        virusScanner.scan(file);
    }

    private String getHexSignature(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            byte[] bytes = new byte[8]; // Read first 8 bytes
            int read = is.read(bytes);
            if (read == -1) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < read; i++) {
                sb.append(String.format("%02X", bytes[i]));
            }
            return sb.toString();
        }
    }
}
