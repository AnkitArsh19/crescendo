package com.crescendo.apps.compression;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

abstract class CompressionHandler implements ActionHandler {

    String value(ActionContext context, String key, String fallback) {
        Object value = context.configuration().get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    ActionResult success(byte[] data, String format) {
        return ActionResult.success(Map.of(
                "format", format,
                "base64", Base64.getEncoder().encodeToString(data),
                "bytes", data.length
        ));
    }

    byte[] gzip(String text) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    byte[] gunzip(byte[] data) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return in.readAllBytes();
        }
    }

    byte[] zip(String fileName, String text) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(fileName == null || fileName.isBlank() ? "data.txt" : fileName));
            zip.write(text.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    byte[] unzipFirst(byte[] data) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry = zip.getNextEntry();
            if (entry == null) {
                throw new IllegalArgumentException("Zip archive has no entries");
            }
            return zip.readAllBytes();
        }
    }
}
