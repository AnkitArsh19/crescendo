package com.crescendo.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Profile({"!prod"})
public class LocalDiskFileStorageService implements FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(LocalDiskFileStorageService.class);

    // Primary persistent directory in user home (.crescendo/uploads)
    private static final Path PRIMARY_STORAGE_PATH = Paths.get(
            System.getProperty("user.home"),
            ".crescendo",
            "uploads"
    );

    // Fallback directories for backward compatibility across platforms / drives
    private static final List<Path> FALLBACK_PATHS = List.of(
            Paths.get(System.getProperty("java.io.tmpdir"), "crescendo-uploads"),
            Paths.get("/tmp/crescendo-uploads/")
    );

    public LocalDiskFileStorageService() {
        try {
            if (!Files.exists(PRIMARY_STORAGE_PATH)) {
                Files.createDirectories(PRIMARY_STORAGE_PATH);
                log.info("Created persistent local storage directory: {}", PRIMARY_STORAGE_PATH.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("Failed to create primary storage directory {}: {}", PRIMARY_STORAGE_PATH, e.getMessage());
        }
    }

    @Override
    public String upload(MultipartFile file, String storageKey) throws IOException {
        if (!Files.exists(PRIMARY_STORAGE_PATH)) {
            Files.createDirectories(PRIMARY_STORAGE_PATH);
        }
        Path targetPath = PRIMARY_STORAGE_PATH.resolve(storageKey);
        try (var in = file.getInputStream(); var out = Files.newOutputStream(targetPath)) {
            in.transferTo(out);
        }
        log.info("Uploaded file '{}' stored at: {}", file.getOriginalFilename(), targetPath.toAbsolutePath());
        return storageKey;
    }

    @Override
    public void delete(String storageKey) {
        Path f = resolvePath(storageKey);
        if (f != null && Files.exists(f)) {
            try {
                Files.delete(f);
                log.info("Deleted local file: {}", storageKey);
            } catch (IOException e) {
                log.warn("Failed to delete local file {}: {}", storageKey, e.getMessage());
            }
        }
    }

    @Override
    public String generateReadUrl(String storageKey, int ttlMinutes) {
        Path p = resolvePath(storageKey);
        return p != null ? p.toUri().toString() : PRIMARY_STORAGE_PATH.resolve(storageKey).toUri().toString();
    }

    @Override
    public void streamContent(String storageKey, OutputStream out) throws IOException {
        Path f = resolvePath(storageKey);
        if (f == null || !Files.exists(f)) {
            throw new IOException("File not found on local disk storage: " + storageKey + " (Checked " + PRIMARY_STORAGE_PATH.toAbsolutePath() + ")");
        }
        try (InputStream in = Files.newInputStream(f)) {
            in.transferTo(out);
        }
    }

    private Path resolvePath(String storageKey) {
        Path primary = PRIMARY_STORAGE_PATH.resolve(storageKey);
        if (Files.exists(primary)) {
            return primary;
        }
        for (Path fallback : FALLBACK_PATHS) {
            try {
                Path fbPath = fallback.resolve(storageKey);
                if (Files.exists(fbPath)) {
                    return fbPath;
                }
            } catch (Exception ignored) {}
        }
        return primary;
    }
}
