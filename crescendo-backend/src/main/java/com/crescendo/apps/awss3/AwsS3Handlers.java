package com.crescendo.apps.awss3;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@ActionMapping(appKey = "aws-s3", actionKey = "list-objects")
class AwsS3ListObjectsHandler extends AwsS3Support {
    @Override
public ActionResult execute(ActionContext c) {
        try (var s = s3(c)) {
            var r = s.listObjectsV2(ListObjectsV2Request.builder().bucket(cfg(c, "bucket")).prefix(cfg(c, "prefix")).build());
            return ActionResult.success(Map.of(
                    "objects", r.contents().stream().map(o -> Map.of(
                            "key", o.key(),
                            "size", o.size(),
                            "lastModified", String.valueOf(o.lastModified())
                    )).toList(),
                    "count", r.keyCount()));
        } catch (Exception e) {
            return ActionResult.failure("S3 list objects failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "aws-s3", actionKey = "put-object")
class AwsS3PutObjectHandler extends AwsS3Support {
    @Override
public ActionResult execute(ActionContext c) {
        try (var s = s3(c)) {
            byte[] b = Base64.getDecoder().decode(cfg(c, "base64"));
            PutObjectRequest.Builder req = PutObjectRequest.builder()
                    .bucket(cfg(c, "bucket"))
                    .key(cfg(c, "key"));
            if (!cfg(c, "contentType").isBlank()) {
                req.contentType(cfg(c, "contentType"));
            }
            if (!cfg(c, "cannedAcl").isBlank()) {
                req.acl(ObjectCannedACL.fromValue(cfg(c, "cannedAcl")));
            }
            s.putObject(req.build(), RequestBody.fromBytes(b));
            return ActionResult.success(Map.of("uploaded", true, "bytes", b.length, "bucket", cfg(c, "bucket"), "key", cfg(c, "key")));
        } catch (Exception e) {
            return ActionResult.failure("S3 put object failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "aws-s3", actionKey = "get-object")
class AwsS3GetObjectHandler extends AwsS3Support {
    @Override
public ActionResult execute(ActionContext c) {
        try (var s = s3(c)) {
            byte[] b = s.getObjectAsBytes(GetObjectRequest.builder().bucket(cfg(c, "bucket")).key(cfg(c, "key")).build()).asByteArray();
            return ActionResult.success(Map.of("base64", Base64.getEncoder().encodeToString(b), "bytes", b.length));
        } catch (Exception e) {
            return ActionResult.failure("S3 get object failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "aws-s3", actionKey = "copy-object")
class AwsS3CopyObjectHandler extends AwsS3Support {
    @Override
public ActionResult execute(ActionContext c) {
        try (var s = s3(c)) {
            s.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(cfg(c, "sourceBucket"))
                    .sourceKey(cfg(c, "sourceKey"))
                    .destinationBucket(cfg(c, "bucket"))
                    .destinationKey(cfg(c, "key"))
                    .build());
            return ActionResult.success(Map.of(
                    "copied", true,
                    "sourceBucket", cfg(c, "sourceBucket"),
                    "sourceKey", cfg(c, "sourceKey"),
                    "bucket", cfg(c, "bucket"),
                    "key", cfg(c, "key")));
        } catch (Exception e) {
            return ActionResult.failure("S3 copy object failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "aws-s3", actionKey = "delete-object")
class AwsS3DeleteObjectHandler extends AwsS3Support {
    @Override
public ActionResult execute(ActionContext c) {
        try (var s = s3(c)) {
            s.deleteObject(b -> b.bucket(cfg(c, "bucket")).key(cfg(c, "key")));
            return ActionResult.success(Map.of("deleted", true, "bucket", cfg(c, "bucket"), "key", cfg(c, "key")));
        } catch (Exception e) {
            return ActionResult.failure("S3 delete object failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "aws-s3", actionKey = "presign-get-object")
class AwsS3PresignGetObjectHandler extends AwsS3Support {
    @Override
public ActionResult execute(ActionContext c) {
        try (var p = presigner(c)) {
            Duration expires = Duration.ofMinutes(Math.max(1, Math.min(intCfg(c, "expiresMinutes", 15), 10080)));
            var request = GetObjectPresignRequest.builder()
                    .signatureDuration(expires)
                    .getObjectRequest(GetObjectRequest.builder().bucket(cfg(c, "bucket")).key(cfg(c, "key")).build())
                    .build();
            var presigned = p.presignGetObject(request);
            return ActionResult.success(Map.of(
                    "url", presigned.url().toString(),
                    "method", presigned.httpRequest().method().name(),
                    "expiresInSeconds", expires.toSeconds()));
        } catch (Exception e) {
            return ActionResult.failure("S3 presign download failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "aws-s3", actionKey = "presign-put-object")
class AwsS3PresignPutObjectHandler extends AwsS3Support {
    @Override
public ActionResult execute(ActionContext c) {
        try (var p = presigner(c)) {
            Duration expires = Duration.ofMinutes(Math.max(1, Math.min(intCfg(c, "expiresMinutes", 15), 10080)));
            PutObjectRequest.Builder put = PutObjectRequest.builder().bucket(cfg(c, "bucket")).key(cfg(c, "key"));
            if (!cfg(c, "contentType").isBlank()) {
                put.contentType(cfg(c, "contentType"));
            }
            var request = PutObjectPresignRequest.builder()
                    .signatureDuration(expires)
                    .putObjectRequest(put.build())
                    .build();
            var presigned = p.presignPutObject(request);
            return ActionResult.success(Map.of(
                    "url", presigned.url().toString(),
                    "method", presigned.httpRequest().method().name(),
                    "headers", presigned.signedHeaders(),
                    "expiresInSeconds", expires.toSeconds()));
        } catch (Exception e) {
            return ActionResult.failure("S3 presign upload failed: " + e.getMessage());
        }
    }
}
