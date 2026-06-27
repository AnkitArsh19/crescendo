package com.crescendo.apps.awss3;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AwsS3FolderHandlers extends AwsS3Support {

    @ActionMapping(appKey = "awss3", actionKey = "list-objects")
    public ActionResult listObjects(ActionContext c) {
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
