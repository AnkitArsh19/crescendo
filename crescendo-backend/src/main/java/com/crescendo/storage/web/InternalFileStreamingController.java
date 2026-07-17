package com.crescendo.storage.web;

import com.crescendo.storage.FileStorageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.context.SecurityContextHolder;
import com.crescendo.security.AppUserDetails;
import com.crescendo.storage.storage_command.UploadedFile_commandRepository;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/internal/files")
public class InternalFileStreamingController {

    private final FileStorageService fileStorageService;
    private final UploadedFile_commandRepository fileRepository;

    public InternalFileStreamingController(FileStorageService fileStorageService, UploadedFile_commandRepository fileRepository) {
        this.fileStorageService = fileStorageService;
        this.fileRepository = fileRepository;
    }

    /**
     * Internal endpoint for Python AI microservices to fetch RETAINED file context.
     * Prevents the need to share presigned URLs or direct S3 access.
     */
    @GetMapping("/{storageKey}")
    public void streamFile(@PathVariable String storageKey, HttpServletResponse response) throws IOException {
        AppUserDetails principal = (AppUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        var file = fileRepository.findById(storageKey)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
            
        if (!file.getUserId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Content-Type would ideally be resolved from UploadedFile_command, 
        // but application/octet-stream is a safe fallback for raw byte streaming
        response.setContentType("application/octet-stream");
        fileStorageService.streamContent(storageKey, response.getOutputStream());
        response.flushBuffer();
    }
}
