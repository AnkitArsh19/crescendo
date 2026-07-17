package com.crescendo.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Profile({"!prod"})
public class LocalDiskFileStorageService implements FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(LocalDiskFileStorageService.class);
    private static final String STORAGE_DIR = "/tmp/crescendo-uploads/";

    public LocalDiskFileStorageService() {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("Created local storage directory: {}", STORAGE_DIR);
            } else {
                log.warn("Failed to create local storage directory: {}", STORAGE_DIR);
            }
        }
    }

    @Override
    public String upload(MultipartFile file, String storageKey) throws IOException {
        Path targetPath = Paths.get(STORAGE_DIR, storageKey);
        try (var in = file.getInputStream(); var out = new FileOutputStream(targetPath.toFile())) {
            in.transferTo(out);
        }
        return storageKey;
    }

    @Override
    public void delete(String storageKey) {
        File f = new File(STORAGE_DIR, storageKey);
        if (f.exists()) {
            if (f.delete()) {
                log.info("Deleted local file: {}", storageKey);
            } else {
                log.warn("Failed to delete local file: {}", storageKey);
            }
        }
    }

    @Override
    public String generateReadUrl(String storageKey, int ttlMinutes) {
        // Return local file URI since presigning doesn't apply to local disk
        return "file://" + STORAGE_DIR + storageKey;
    }

    @Override
    public void streamContent(String storageKey, OutputStream out) throws IOException {
        File f = new File(STORAGE_DIR, storageKey);
        if (!f.exists()) {
            throw new IOException("File not found: " + storageKey);
        }
        try (FileInputStream fis = new FileInputStream(f)) {
            fis.transferTo(out);
        }
    }
}
