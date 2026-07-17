package com.crescendo.storage.security;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A stub implementation of VirusScanner. 
 * Provides a hook to seamlessly swap in a ClamAV implementation in the future.
 */
@Component
public class NoOpVirusScanner implements VirusScanner {
    private static final Logger log = LoggerFactory.getLogger(NoOpVirusScanner.class);

    @Override
    public void scan(MultipartFile file) {
        log.debug("NoOpVirusScanner skipping scan for file: {}", file.getOriginalFilename());
        // Do nothing. Assume clean.
    }
}
