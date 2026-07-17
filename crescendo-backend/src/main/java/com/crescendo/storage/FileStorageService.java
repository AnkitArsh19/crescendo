package com.crescendo.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.OutputStream;
import java.io.IOException;

public interface FileStorageService {
    /**
     * Uploads the file and returns the unique storage key.
     */
    String upload(MultipartFile file, String storageKey) throws IOException;

    /**
     * Deletes the file by its storage key.
     */
    void delete(String storageKey);

    /**
     * Generates a read-only URL valid for the specified TTL (in minutes).
     * Returns a presigned URL (for S3) or a local file:// URI (for dev).
     */
    String generateReadUrl(String storageKey, int ttlMinutes);

    /**
     * Streams the content of the file to the given OutputStream.
     * Used for internal access (e.g., AI microservice streaming endpoint)
     * where providing a presigned URL is inappropriate.
     */
    void streamContent(String storageKey, OutputStream out) throws IOException;
}
