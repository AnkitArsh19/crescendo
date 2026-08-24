package com.crescendo.storage.security;

import com.crescendo.storage.storage_command.ConsumptionModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StorageSecurityValidationTest {

    private FileValidationService fileValidationService;
    private UrlSecurityValidator urlSecurityValidator;

    @BeforeEach
    void setUp() {
        fileValidationService = new FileValidationService(new NoOpVirusScanner());
        urlSecurityValidator = new UrlSecurityValidator();
    }

    @Test
    @DisplayName("Valid video and image files pass validation")
    void testValidMediaFilesPass() {
        // MP4 file signature (ftyp)
        byte[] mp4Bytes = new byte[] { 0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x6D, 0x70, 0x34, 0x32 };
        MockMultipartFile mp4File = new MockMultipartFile("file", "presentation.mp4", "video/mp4", mp4Bytes);
        assertDoesNotThrow(() -> fileValidationService.validate(mp4File, ConsumptionModel.RELAY, 50 * 1024 * 1024));

        // PNG file signature
        byte[] pngBytes = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        MockMultipartFile pngFile = new MockMultipartFile("file", "photo.png", "image/png", pngBytes);
        assertDoesNotThrow(() -> fileValidationService.validate(pngFile, ConsumptionModel.RETAINED, 50 * 1024 * 1024));
    }

    @Test
    @DisplayName("Executable magic bytes (Windows PE, Linux ELF, Java Bytecode, Shell scripts) are blocked")
    void testExecutableSignaturesBlocked() {
        // Windows EXE (MZ header)
        byte[] exeBytes = new byte[] { 0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00 };
        MockMultipartFile exeFile = new MockMultipartFile("file", "video.mp4", "video/mp4", exeBytes);
        SecurityException ex1 = assertThrows(SecurityException.class,
                () -> fileValidationService.validate(exeFile, ConsumptionModel.RELAY, 50 * 1024 * 1024));
        assertTrue(ex1.getMessage().contains("strictly prohibited"));

        // Linux ELF
        byte[] elfBytes = new byte[] { 0x7F, 0x45, 0x4C, 0x46, 0x02, 0x01, 0x01, 0x00 };
        MockMultipartFile elfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", elfBytes);
        assertThrows(SecurityException.class,
                () -> fileValidationService.validate(elfFile, ConsumptionModel.RELAY, 50 * 1024 * 1024));

        // Shell script (#!)
        byte[] shBytes = "#!/bin/bash\nrm -rf /".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile shFile = new MockMultipartFile("file", "script.sh", "text/plain", shBytes);
        assertThrows(SecurityException.class,
                () -> fileValidationService.validate(shFile, ConsumptionModel.RELAY, 50 * 1024 * 1024));
    }

    @Test
    @DisplayName("Dangerous double extensions and path traversal filenames are blocked")
    void testDangerousFilenamesBlocked() {
        byte[] dummyBytes = new byte[] { 0x01, 0x02, 0x03, 0x04 };

        // Double extension (e.g. funny_clip.mp4.exe)
        MockMultipartFile doubleExt = new MockMultipartFile("file", "funny_clip.mp4.exe", "video/mp4", dummyBytes);
        assertThrows(SecurityException.class,
                () -> fileValidationService.validate(doubleExt, ConsumptionModel.RELAY, 50 * 1024 * 1024));

        // Path traversal
        MockMultipartFile pathTraversal = new MockMultipartFile("file", "../../etc/passwd", "text/plain", dummyBytes);
        assertThrows(SecurityException.class,
                () -> fileValidationService.validate(pathTraversal, ConsumptionModel.RELAY, 50 * 1024 * 1024));
    }

    @Test
    @DisplayName("SVG containing XSS script payloads is blocked")
    void testSvgXssBlocked() {
        String maliciousSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(document.cookie)</script></svg>";
        MockMultipartFile svgFile = new MockMultipartFile("file", "icon.svg", "image/svg+xml", maliciousSvg.getBytes(StandardCharsets.UTF_8));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> fileValidationService.validate(svgFile, ConsumptionModel.RELAY, 50 * 1024 * 1024));
        assertTrue(ex.getMessage().contains("disallowed executable scripts"));
    }

    @Test
    @DisplayName("SSRF Validator blocks loopback, private RFC1918, AWS metadata, and non-HTTP protocols")
    void testSsrfProtection() {
        // Disallowed protocol schemes
        assertThrows(SecurityException.class, () -> urlSecurityValidator.validateUrl("file:///etc/passwd"));
        assertThrows(SecurityException.class, () -> urlSecurityValidator.validateUrl("gopher://127.0.0.1:6379"));
        assertThrows(SecurityException.class, () -> urlSecurityValidator.validateUrl("ftp://files.example.com"));

        // Loopback & Localhost
        assertThrows(SecurityException.class, () -> urlSecurityValidator.validateUrl("http://localhost:8080/admin"));
        assertThrows(SecurityException.class, () -> urlSecurityValidator.validateUrl("http://127.0.0.1:8080/actuator"));

        // AWS/Cloud IMDS metadata
        assertThrows(SecurityException.class, () -> urlSecurityValidator.validateUrl("http://169.254.169.254/latest/meta-data/"));

        // Embedded credentials
        assertThrows(SecurityException.class, () -> urlSecurityValidator.validateUrl("http://admin:secret@malicious.com"));
    }
}
