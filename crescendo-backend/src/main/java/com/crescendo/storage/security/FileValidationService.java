package com.crescendo.storage.security;

import com.crescendo.storage.storage_command.ConsumptionModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class FileValidationService {
    private static final Logger log = LoggerFactory.getLogger(FileValidationService.class);

    private final VirusScanner virusScanner;

    // Hardcoded executable and script denylist (magic byte hex prefixes)
    private static final Set<String> DENYLIST_MAGIC_BYTES = Set.of(
            "4D5A",         // .exe, .dll, .sys, .com (Windows PE)
            "7F454C46",     // .elf (Linux executable)
            "FEEDFACE",     // Mach-O binary (32-bit Mac)
            "FEEDFACF",     // Mach-O binary (64-bit Mac)
            "CAFEBABE",     // Java bytecode .class / Universal Mach-O
            "2321",         // #! (Shell scripts, e.g. #!/bin/bash, #!/usr/bin/env)
            "3C3F706870",   // <?php (PHP scripts)
            "3C25",         // <% (JSP/ASP scripts)
            "3C7363726970"  // <scrip (<script HTML/XSS tag)
    );

    // Dangerous file extensions that must never be uploaded directly
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "dll", "com", "bat", "cmd", "ps1", "psm1", "vbs", "vbe", "js", "jse", "wsf", "wsh",
            "sh", "bash", "zsh", "elf", "bin", "app", "dmg", "pkg", "deb", "rpm", "msi", "msp",
            "jar", "war", "ear", "class",
            "php", "phtml", "php3", "php4", "php5", "phps", "asp", "aspx", "jsp", "jspx", "cgi", "pl", "py"
    );

    // Strict allowlist for RETAINED AI context (PDFs, images)
    private static final Set<String> RETAINED_ALLOWLIST = Set.of(
            "25504446", // .pdf
            "FFD8FF",   // .jpg, .jpeg
            "89504E47", // .png
            "52494646"  // .webp / RIFF
    );

    // SVG XSS detection patterns
    private static final Pattern SVG_SCRIPT_PATTERN = Pattern.compile(
            "(?i)<script|javascript:|onload\\s*=|onerror\\s*=|onclick\\s*=|onmouseover\\s*=|xlink:href\\s*=\\s*['\"]javascript:|<!ENTITY"
    );

    public FileValidationService(VirusScanner virusScanner) {
        this.virusScanner = virusScanner;
    }

    public void validate(MultipartFile file, ConsumptionModel consumptionModel, long maxAllowedSize) throws IOException, SecurityException {
        // 1. Basic Presence & Size Check
        if (file == null || file.isEmpty()) {
            throw new SecurityException("Uploaded file cannot be empty");
        }

        if (file.getSize() > maxAllowedSize) {
            throw new SecurityException("File size (" + (file.getSize() / (1024 * 1024)) + "MB) exceeds allowed limit of " + (maxAllowedSize / (1024 * 1024)) + "MB");
        }

        // 2. Filename & Extension Sanitization
        String originalFilename = file.getOriginalFilename();
        validateFilename(originalFilename);

        // 3. Magic Bytes Check
        String hexSignature = getHexSignature(file);
        
        // 4. Executable / Script Denylist Check
        for (String denied : DENYLIST_MAGIC_BYTES) {
            if (hexSignature.startsWith(denied)) {
                log.warn("Blocked executable/script upload attempt. File: '{}', Magic Signature: {}", originalFilename, hexSignature);
                throw new SecurityException("Executable, script, and bytecode files are strictly prohibited");
            }
        }

        // 5. SVG / XML Stored XSS Inspection
        if (originalFilename != null && originalFilename.toLowerCase(Locale.ROOT).endsWith(".svg")) {
            validateSvgContent(file);
        }

        // 6. Strict Allowlist for RETAINED files
        if (consumptionModel == ConsumptionModel.RETAINED) {
            boolean allowed = false;
            for (String allowedSig : RETAINED_ALLOWLIST) {
                if (hexSignature.startsWith(allowedSig)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed && file.getContentType() != null && file.getContentType().startsWith("text/")) {
                allowed = true;
            }

            if (!allowed) {
                log.warn("Blocked RETAINED upload of unauthorized type. Signature: {}", hexSignature);
                throw new SecurityException("File type not allowed for AI context retention");
            }
        }

        // 7. Virus Scan (ClamAV / Signature scanner)
        virusScanner.scan(file);
    }

    /**
     * Validates filename against path traversal, null byte injections, and dangerous double extensions.
     */
    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }

        // Path traversal checks
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || filename.contains("\0")) {
            log.warn("Blocked filename containing path traversal or null byte: '{}'", filename);
            throw new SecurityException("Invalid characters or path traversal sequence in filename");
        }

        String lower = filename.toLowerCase(Locale.ROOT);

        // Check single or double extensions (e.g. video.mp4.exe)
        String[] parts = lower.split("\\.");
        if (parts.length > 1) {
            String primaryExt = parts[parts.length - 1];
            if (BLOCKED_EXTENSIONS.contains(primaryExt)) {
                log.warn("Blocked file upload with forbidden extension: '{}'", primaryExt);
                throw new SecurityException("File extension '." + primaryExt + "' is not permitted");
            }

            // Check if user is attempting a double extension trick (e.g. payload.exe.mp4 is caught by magic bytes, but file.mp4.bat is caught here)
            for (int i = 1; i < parts.length; i++) {
                if (BLOCKED_EXTENSIONS.contains(parts[i])) {
                    log.warn("Blocked suspicious multi-extension filename: '{}'", filename);
                    throw new SecurityException("Multi-extension executable filenames are prohibited");
                }
            }
        }
    }

    /**
     * Inspects SVG files for embedded JavaScript, event handlers, or external entity expansion (XXE/XSS).
     */
    private void validateSvgContent(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = is.readNBytes(16384); // Read first 16KB of SVG
            String content = new String(buffer, StandardCharsets.UTF_8);
            if (SVG_SCRIPT_PATTERN.matcher(content).find()) {
                log.warn("Blocked SVG containing malicious script or event handler payload");
                throw new SecurityException("SVG contains disallowed executable scripts or event handlers");
            }
        }
    }

    private String getHexSignature(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            byte[] bytes = new byte[8];
            int read = is.read(bytes);
            if (read <= 0) {
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
