package com.crescendo.apps.awss3;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AwsS3App implements AppDefinition {
    @Override
    public App toApp() {
        return new App("aws-s3", "AWS S3", "List, upload, download, copy, delete, and presign S3 objects",
                "/icons/aws-s3.svg", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "list-objects", "name", "List Objects", "description", "List objects in a bucket",
                                "configSchema", List.of(
                                        Map.of("key", "bucket", "label", "Bucket", "type", "text", "required", true),
                                        Map.of("key", "prefix", "label", "Prefix", "type", "text", "required", false))),
                        Map.of("actionKey", "put-object", "name", "Put Object", "description", "Upload Base64 data",
                                "configSchema", List.of(
                                        Map.of("key", "bucket", "label", "Bucket", "type", "text", "required", true),
                                        Map.of("key", "key", "label", "Key", "type", "text", "required", true),
                                        Map.of("key", "base64", "label", "Base64 Data", "type", "textarea", "required", true),
                                        Map.of("key", "contentType", "label", "Content Type", "type", "text", "required", false),
                                        Map.of("key", "cannedAcl", "label", "Canned ACL", "type", "select", "required", false,
                                                "options", List.of(
                                                        Map.of("value", "", "label", "Bucket default"),
                                                        Map.of("value", "private", "label", "Private"),
                                                        Map.of("value", "public-read", "label", "Public read"))))),
                        Map.of("actionKey", "get-object", "name", "Get Object", "description", "Download object as Base64",
                                "configSchema", List.of(
                                        Map.of("key", "bucket", "label", "Bucket", "type", "text", "required", true),
                                        Map.of("key", "key", "label", "Key", "type", "text", "required", true))),
                        Map.of("actionKey", "copy-object", "name", "Copy Object", "description", "Copy an object between S3 keys or buckets",
                                "configSchema", List.of(
                                        Map.of("key", "sourceBucket", "label", "Source Bucket", "type", "text", "required", true),
                                        Map.of("key", "sourceKey", "label", "Source Key", "type", "text", "required", true),
                                        Map.of("key", "bucket", "label", "Destination Bucket", "type", "text", "required", true),
                                        Map.of("key", "key", "label", "Destination Key", "type", "text", "required", true))),
                        Map.of("actionKey", "delete-object", "name", "Delete Object", "description", "Delete an object",
                                "configSchema", List.of(
                                        Map.of("key", "bucket", "label", "Bucket", "type", "text", "required", true),
                                        Map.of("key", "key", "label", "Key", "type", "text", "required", true))),
                        Map.of("actionKey", "presign-get-object", "name", "Create Download URL", "description", "Create a temporary presigned download URL",
                                "configSchema", List.of(
                                        Map.of("key", "bucket", "label", "Bucket", "type", "text", "required", true),
                                        Map.of("key", "key", "label", "Key", "type", "text", "required", true),
                                        Map.of("key", "expiresMinutes", "label", "Expires Minutes", "type", "text", "required", false, "placeholder", "15"))),
                        Map.of("actionKey", "presign-put-object", "name", "Create Upload URL", "description", "Create a temporary presigned upload URL",
                                "configSchema", List.of(
                                        Map.of("key", "bucket", "label", "Bucket", "type", "text", "required", true),
                                        Map.of("key", "key", "label", "Key", "type", "text", "required", true),
                                        Map.of("key", "contentType", "label", "Content Type", "type", "text", "required", false),
                                        Map.of("key", "expiresMinutes", "label", "Expires Minutes", "type", "text", "required", false, "placeholder", "15")))
                )
        ).credentialSchema(List.of(
                        Map.of("key", "accessKeyId", "label", "Access Key ID", "type", "password", "required", true),
                        Map.of("key", "secretAccessKey", "label", "Secret Access Key", "type", "password", "required", true),
                        Map.of("key", "region", "label", "Region", "type", "text", "required", true, "placeholder", "us-east-1"),
                        Map.of("key", "endpoint", "label", "Custom Endpoint", "type", "text", "required", false)))
                .category("file-storage")
                .helpUrl("https://docs.aws.amazon.com/AmazonS3/latest/API/Welcome.html");
    }
}
