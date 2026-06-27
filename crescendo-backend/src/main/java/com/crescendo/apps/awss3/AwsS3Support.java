package com.crescendo.apps.awss3;

import com.crescendo.execution.action.ActionContext;
// import com.crescendo.execution.action.ActionHandler;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

abstract class AwsS3Support {
    S3Client s3(ActionContext c) {
        var b = S3Client.builder()
                .credentialsProvider(credentials(c))
                .region(region(c));
        String ep = cred(c, "endpoint");
        if (!ep.isBlank()) {
            b.endpointOverride(URI.create(ep));
        }
        return b.build();
    }

    S3Presigner presigner(ActionContext c) {
        var b = S3Presigner.builder()
                .credentialsProvider(credentials(c))
                .region(region(c));
        String ep = cred(c, "endpoint");
        if (!ep.isBlank()) {
            b.endpointOverride(URI.create(ep));
        }
        return b.build();
    }

    String cfg(ActionContext c, String k) {
        Object v = c.configuration().get(k);
        return v == null ? "" : String.valueOf(v);
    }

    int intCfg(ActionContext c, String k, int fallback) {
        try {
            String v = cfg(c, k);
            return v.isBlank() ? fallback : Integer.parseInt(v);
        } catch (Exception e) {
            return fallback;
        }
    }

    String cred(ActionContext c, String k) {
        Object v = c.credentials() != null ? c.credentials().get(k) : null;
        return v == null ? "" : String.valueOf(v);
    }

    private StaticCredentialsProvider credentials(ActionContext c) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                cred(c, "accessKeyId"), cred(c, "secretAccessKey")));
    }

    private Region region(ActionContext c) {
        return Region.of(cred(c, "region"));
    }
}
