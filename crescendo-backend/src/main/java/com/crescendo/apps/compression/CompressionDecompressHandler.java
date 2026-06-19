package com.crescendo.apps.compression;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@ActionMapping(appKey = "compression", actionKey = "decompress")
public class CompressionDecompressHandler extends CompressionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        try {
            String format = value(context, "format", "gzip").trim().toLowerCase();
            String base64 = value(context, "base64", "");
            if (base64.isBlank()) {
                return ActionResult.failure("Compression base64 data is required");
            }
            byte[] compressed = Base64.getDecoder().decode(base64);
            byte[] bytes = "zip".equals(format) ? unzipFirst(compressed) : gunzip(compressed);
            return ActionResult.success(Map.of(
                    "format", format,
                    "text", new String(bytes, StandardCharsets.UTF_8),
                    "bytes", bytes.length
            ));
        } catch (Exception e) {
            return ActionResult.failure("Decompression failed: " + e.getMessage());
        }
    }
}
