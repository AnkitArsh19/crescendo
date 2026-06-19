package com.crescendo.apps.compression;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;

@ActionMapping(appKey = "compression", actionKey = "compress")
public class CompressionCompressHandler extends CompressionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        try {
            String format = value(context, "format", "gzip").trim().toLowerCase();
            String text = value(context, "text", "");
            if ("zip".equals(format)) {
                return success(zip(value(context, "fileName", "data.txt"), text), "zip");
            }
            return success(gzip(text), "gzip");
        } catch (Exception e) {
            return ActionResult.failure("Compression failed: " + e.getMessage());
        }
    }
}
