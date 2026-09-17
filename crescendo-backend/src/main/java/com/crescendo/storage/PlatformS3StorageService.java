package com.crescendo.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;

@Service
@Profile("prod")
public class PlatformS3StorageService implements FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(PlatformS3StorageService.class);

    @Value("${crescendo.storage.s3.bucket}")
    private String bucket;

    @Value("${crescendo.storage.s3.region}")
    private String region;

    @Value("${crescendo.storage.s3.access-key:}")
    private String accessKey;

    @Value("${crescendo.storage.s3.secret-key:}")
    private String secretKey;

    @Value("${crescendo.storage.s3.endpoint:}")
    private String endpoint;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    public void init() {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region));
        S3Presigner.Builder presignerBuilder = S3Presigner.builder().region(Region.of(region));

        if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
            StaticCredentialsProvider creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
            builder.credentialsProvider(creds);
            presignerBuilder.credentialsProvider(creds);
        }

        if (!endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
            presignerBuilder.endpointOverride(URI.create(endpoint));
        }

        this.s3Client = builder.build();
        this.s3Presigner = presignerBuilder.build();
        log.info("Initialized PlatformS3StorageService for bucket: {}", bucket);
    }

    private final LocalDiskFileStorageService localFallback = new LocalDiskFileStorageService();

    @Override
    public String upload(MultipartFile file, String storageKey) throws IOException {
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return storageKey;
        } catch (Exception e) {
            log.warn("AWS S3 upload to bucket '{}' failed: {}. Falling back to local disk storage.", bucket, e.getMessage());
            return localFallback.upload(file, storageKey);
        }
    }

    @Override
    public void delete(String storageKey) {
        localFallback.delete(storageKey);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
            log.info("Deleted S3 object: {}/{}", bucket, storageKey);
        } catch (Exception e) {
            log.warn("Failed to delete S3 object: {}/{}", bucket, storageKey, e);
        }
    }

    @Override
    public String generateReadUrl(String storageKey, int ttlMinutes) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(storageKey).build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(ttlMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.warn("Failed to generate presigned S3 URL for '{}': {}. Falling back to local read URL.", storageKey, e.getMessage());
            return localFallback.generateReadUrl(storageKey, ttlMinutes);
        }
    }

    @Override
    public void streamContent(String storageKey, OutputStream out) throws IOException {
        try {
            localFallback.streamContent(storageKey, out);
            return;
        } catch (Exception ignored) {
            // Not on local disk, attempt S3
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(storageKey).build();
            try (var in = s3Client.getObject(getObjectRequest)) {
                in.transferTo(out);
                return;
            }
        } catch (Exception e) {
            log.warn("S3 streamContent failed for key '{}' in bucket '{}': {}", storageKey, bucket, e.getMessage());
        }

        throw new IOException("File '" + storageKey + "' not found in storage. If this file was uploaded before server update/restart, please re-upload the file in the step configuration.");
    }
}
