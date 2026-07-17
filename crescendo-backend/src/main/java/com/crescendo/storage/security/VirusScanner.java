package com.crescendo.storage.security;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface VirusScanner {
    /**
     * Scans the given file for malicious content.
     *
     * @param file the file to scan
     * @throws SecurityException if the file is malicious or scanning fails
     */
    void scan(MultipartFile file) throws IOException, SecurityException;
}
