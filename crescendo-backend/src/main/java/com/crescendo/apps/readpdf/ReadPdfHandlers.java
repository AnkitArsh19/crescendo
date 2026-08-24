package com.crescendo.apps.readpdf;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.storage.MediaStreamResolver;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@Component
public class ReadPdfHandlers {

    private final MediaStreamResolver mediaStreamResolver;

    public ReadPdfHandlers() {
        this(new MediaStreamResolver());
    }

    @Autowired
    public ReadPdfHandlers(MediaStreamResolver mediaStreamResolver) {
        this.mediaStreamResolver = mediaStreamResolver;
    }

    @ActionMapping(appKey = "readpdf", actionKey = "extract-text")
    public ActionResult extractText(ActionContext c) {
        try {
            Map<String, Object> config = c.configuration();
            Object rawFile = config.get("file");
            if (rawFile == null) rawFile = config.get("fileUrl");
            if (rawFile == null) rawFile = config.get("base64");

            if (rawFile == null) {
                return ActionResult.failure("PDF file or URL is required");
            }

            byte[] bytes;
            try (MediaStreamResolver.MediaSource media = mediaStreamResolver.resolve(rawFile, "application/pdf")) {
                InputStream in = media.stream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                in.transferTo(baos);
                bytes = baos.toByteArray();
            }

            String pass = String.valueOf(config.getOrDefault("password", ""));
            String textPropertyName = String.valueOf(config.getOrDefault("textPropertyName", "text"));

            try (var doc = pass.isBlank() ? Loader.loadPDF(bytes) : Loader.loadPDF(bytes, pass)) {
                String text = new PDFTextStripper().getText(doc);
                return ActionResult.success(Map.of(textPropertyName, text, "pages", doc.getNumberOfPages()));
            }
        } catch (Exception e) {
            return ActionResult.failure("Read PDF failed: " + e.getMessage());
        }
    }
}
