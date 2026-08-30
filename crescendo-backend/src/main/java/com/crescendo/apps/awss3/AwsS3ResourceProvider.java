package com.crescendo.apps.awss3;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.net.URI;
import java.util.*;

/**
 * Fetches AWS S3 resources: buckets and objects inside a bucket.
 */
@Component
public class AwsS3ResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(AwsS3ResourceProvider.class);

    @Override
    public String appKey() {
        return "awss3";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("buckets", "objects");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("buckets", 100, java.time.Duration.ofMinutes(10)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        S3Client s3Client = buildS3Client(credentials);
        try {
            return switch (resourceType) {
                case "buckets" -> listBuckets(s3Client);
                case "objects" -> listObjects(s3Client, params.getOrDefault("bucket", ""));
                default -> List.of();
            };
        } finally {
            try {
                s3Client.close();
            } catch (Exception ignored) {
            }
        }
    }

    private List<ResourceOption> listBuckets(S3Client s3Client) {
        try {
            ListBucketsResponse resp = s3Client.listBuckets();
            return resp.buckets().stream()
                    .map(b -> new ResourceOption(b.name(), b.name(), "S3 Bucket"))
                    .toList();
        } catch (Exception e) {
            logger.error("[awss3] Failed to list buckets: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listObjects(S3Client s3Client, String bucket) {
        if (bucket == null || bucket.isBlank()) return List.of();
        try {
            ListObjectsV2Response resp = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .maxKeys(100)
                    .build());
            return resp.contents().stream()
                    .map(o -> new ResourceOption(o.key(), o.key(), (o.size() / 1024) + " KB"))
                    .toList();
        } catch (Exception e) {
            logger.error("[awss3] Failed to list objects in bucket {}: {}", bucket, e.getMessage());
            return List.of();
        }
    }

    private S3Client buildS3Client(Map<String, Object> credentials) {
        String accessKey = str(credentials.get("accessKeyId"));
        String secretKey = str(credentials.get("secretAccessKey"));
        String regionStr = str(credentials.get("region"));
        String endpoint = str(credentials.get("endpoint"));

        if (accessKey.isBlank() || secretKey.isBlank()) {
            throw new IllegalArgumentException("AWS S3 requires accessKeyId and secretAccessKey.");
        }

        Region region = regionStr.isBlank() ? Region.US_EAST_1 : Region.of(regionStr);
        S3ClientBuilder builder = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
